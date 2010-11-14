(ns hozumi.test-session-expiry
  (:use [hozumi.session-expiry] :reload)
  (:use [ring.middleware.session])
  (:use [clojure.test])
  (:import [java.util Date]))

(defn- handler [req]
  (condp = (:uri req)
      "/change" (let [v (.toString (Math/random))]
		  {:session {:myvalue v}
		   :body {:before (-> req :session :myvalue)
			  :after v
			  :timestamp (-> req :session :session_timestamp)}})
      "/stay"   {:body {:before (-> req :session :myvalue)
			:after (-> req :session :myvalue)
			:timestamp (-> req :session :session_timestamp)}}
      "/remove" {:session nil
		 :body {:before (-> req :session :myvalue)
			:after nil
			:timestamp (-> req :session :session_timestamp)}}))

(def app (-> handler
	     (wrap-session-expiry 2)
	     wrap-session))

(defn- get-session-id [response]
  (->> (-> response
	   (get-in [:headers "Set-Cookie"])
	   first)
       (re-matches #"ring-session=([^;]*);.*")
       second))

(deftest test-expiry
  (let [response1 (app {:uri "/change"})
	sess-key (get-session-id response1)
	response2 (app {:uri "/stay"
			:cookies {"ring-session" {:value sess-key}}})
	response3 (app {:uri "/change"
			:cookies {"ring-session" {:value sess-key}}})]
    (is (get-in response1 [:headers "Set-Cookie"]))
    (is (nil? (get-in response2 [:headers "Set-Cookie"])))
    (is (= (-> response1 :body :after)
	   (-> response2 :body :after)))
    (is (nil? (get-in response3 [:headers "Set-Cookie"])))
    (is (not= (-> response1 :body :after)
	      (-> response3 :body :after)))
    (Thread/sleep 3000)
    (let [response4 (app {:uri "/stay"
			  :cookies {"ring-session" {:value sess-key}}})
	  response5 (app {:uri "/change"
			  :cookies {"ring-session" {:value sess-key}}})
	  response6 (app {:uri "/stay"
			  :cookies {"ring-session" {:value sess-key}}})]
      (is (nil? (get-in response4 [:headers "Set-Cookie"])))
      (is (nil? (-> response4 :body :before)))
      (is (not= (-> response3 :body :after)
		(-> response4 :body :before)))
      (is (not= (-> response3 :body :after)
		(-> response5 :body :before)))
      (is (= (-> response5 :body :after)
	     (-> response6 :body :after))))))

(deftest test-not-expiry
  (let [response1 (app {:uri "/change"})
	sess-key (get-session-id response1)]
    (is (-> response1 :body :after))
    (Thread/sleep 1500)
    (app {:uri "/stay"
	  :cookies {"ring-session" {:value sess-key}}})
    (Thread/sleep 1500)
    (let [response2 (app {:uri "/stay"
			  :cookies {"ring-session" {:value sess-key}}})]
      (is (not= (-> response1 :body :timestamp)
		(-> response2 :body :timestamp)))
      (is (= (-> response1 :body :after)
	     (-> response2 :body :after))))))

(deftest test-remove-session
  (let [response1 (app {:uri "/change"})
	sess-key  (get-session-id response1)
	response2 (app {:uri "/remove"
			:cookies {"ring-session" {:value sess-key}}})
	response3 (app {:uri "/stay"
			:cookies {"ring-session" {:value sess-key}}})]
    (is (-> response1 :body :after))
    (is (nil? (get-in response2 [:headers "Set-Cookie"])))
    (is (nil? (-> response3 :body :before)))))

(deftest test-no-session
  (let [response1 (app {:uri "/stay"})]
    (is (nil? (get-in response1 [:headers "Set-Cookie"])))))

