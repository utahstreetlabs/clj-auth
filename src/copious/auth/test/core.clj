(ns copious.auth.test.core
  (:use [copious.auth.core])
  (:use [midje.sweet]))

(set-environment! :test)

(fact (bucket-key :facebook "12345") => ["test-identities" "facebook:12345"])

(fact
  (decode (encode {:token "deadbeef" :token_expires_at 19182012 :user_id 12}))
  => (contains {:token "deadbeef"}))

(fact
  (merge-new-data {} {:tmp_token "deadbeef" :tmp_token_expires_at 19182012 :user_id 12})
  => (contains {:token "deadbeef"}))

(fact
  (merge-new-data {:token "cafebebe"
                   :token_expires_at 19182000
                   :user_id 12}
                  {:tmp_token "deadbeef"
                   :tmp_token_expires_at 19182012})
  => (contains {:token "deadbeef"}))
