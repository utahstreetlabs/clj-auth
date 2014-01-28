(ns copious.auth.core
  (:require [clojure.data.json :as json]
            [clojure.string :as string]
            [clojurewerkz.welle.core :as wc]
            [clojurewerkz.welle.buckets :as wb]
            [clojurewerkz.welle.kv :as kv]
            [copious.auth.config :as c])
  (:import com.google.protobuf.Descriptors$FieldDescriptor$Type
           com.copious.auth.Auth$Identity))

(defn set-environment! [env]
  (c/set-environment! env))

;; there is some confusing naming in welle / java riak client, where welle's `connect` methods return a client,
;; which manages a connection pool.
(declare riak-client)

(defmacro with-riak [& forms]
  `(do
    (when (not (bound? #'riak-client))
      (alter-var-root #'riak-client (constantly (wc/connect-to-cluster-via-pb (c/riak)))))
    (wc/with-client riak-client (do ~@forms))))

(defn build [data]
  (let [builder (Auth$Identity/newBuilder)
        {:keys [token token_expires_at tmp_token tmp_token_expires_at secret code scope user_id]} data]
    (.setToken builder token)
    (.setUserId builder user_id)
    (if scope (.setScope builder scope))
    (if secret (.setSecret builder secret))
    (if token_expires_at (.setTokenExpiresAt builder token_expires_at))
    (if tmp_token (.setTmpToken builder tmp_token))
    (if tmp_token_expires_at (.setTmpTokenExpiresAt builder tmp_token_expires_at))
    (if code (.setCode builder code))
    (.build builder)))

(defn encode [data]
  (.toByteArray (build data)))

(defn decode-field [field]
  (let [[k v] [(.getKey field) (.getValue field)]]
    [(keyword (.getName k))
     (if (= (.getType k) (Descriptors$FieldDescriptor$Type/ENUM))
       (string/lower-case (.getName v))
       v)]))

(defn decode [data]
  (let [iden (Auth$Identity/parseFrom data)]
    (reduce #(let [[k v] (decode-field %2)] (assoc %1 k v)) {} (seq (.getAllFields iden)))))

(defn bucket []
  (str (name c/environment) "-identities"))

(defn bucket-key [provider uid]
  [(bucket) (str (name provider) ":" uid)])

(defn choose-token-and-expiry [current-token current-expiry new-token new-expiry]
  (if (or (nil? current-expiry) (< current-expiry new-expiry))
    [new-token new-expiry]
    [current-token current-expiry]))

(defn merge-new-data [current new-data]
  "Merge new identity data into existing, ensuring that a token with a shorter expiry doesn't overwrite one that
   would last longer"
  (let [new-token (or (:token new-data) (:tmp_token new-data))
        new-expiry (or (:token_expires_at new-data) (:tmp_token_expires_at new-data))
        [token-to-use expiry-to-use]
          (choose-token-and-expiry (:token current) (:token_expires_at current) new-token new-expiry)]
    (merge current new-data {:token token-to-use :token_expires_at expiry-to-use})))

(defn real-fetch [bucket key]
  (let [[val] (with-riak (kv/fetch bucket key))
        bytes (:value val)]
    (and bytes (decode bytes))))

(defn fetch [provider uid]
  (let [[bucket key] (bucket-key provider uid)]
    (real-fetch bucket key)))

(defn real-store [bucket key data]
  (let [merged (merge-new-data (or (real-fetch bucket key) {}) data)]
    (with-riak (kv/store bucket key (encode merged)))
    merged))

(defn store [data]
  (let [[bucket key] (bucket-key (:provider data) (:uid data))]
    (real-store bucket key data)))

(defn delete [provider uid]
  (with-riak (apply kv/delete (bucket-key provider uid))))

(defn delete-all []
  (with-riak
    (let [keys (wb/keys-in (bucket))]
      (doseq [k keys] (kv/delete (bucket) k)))))
