(ns copious.auth.config)

(declare environment)

(defn set-environment! [env]
  (alter-var-root #'environment (constantly (keyword env))))

(def db
  {:development
    {:riak ["127.0.0.1"]}
   :test
    {:riak ["127.0.0.1"]}
   :staging
   {:riak ["staging3.copious.com"]}
   :demo
   {:riak ["demo3.copious.com"]}
   :production
   {:riak ["riak1.copious.com" "riak2.copious.com" "riak3.copious.com"]}})

(defn riak [] (:riak (db environment)))
