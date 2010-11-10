(ns hozumi.session-expiry
  (:import [java.util Date]))

(defn- expire? [date expire-sec]
  (< (* 1000 expire-sec)
     (- (.getTime (Date.)) (.getTime date))))

(defn wrap-session-expiry [handler expire-sec]
  (fn [{{date ::date :as req-session} :session :as request}]
    (let [request  (if (and date (expire? date expire-sec))
		     (assoc request :session {})
		     request)
	  response (handler request)]
      (if (contains? response :session)
	(if (response :session)
	  (assoc-in response [:session ::date] (Date.))
	  response)
	(if (empty? req-session)
	  response
	  (assoc response :session (assoc req-session ::date (Date.))))))))
