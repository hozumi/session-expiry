(ns hozumi.session-expiry
  (:import [java.util Date]))

(defn- expire? [date expire-ms]
  (< expire-ms
     (- (.getTime (Date.)) (.getTime date))))

(defn wrap-session-expiry [handler expire-sec]
  (let [expire-ms (* 1000 expire-sec)]
    (fn [{{timestamp :session_timestamp :as req-session} :session :as request}]
      (let [expired?  (and timestamp (expire? timestamp expire-ms))
	    request  (if expired?
		       (assoc request :session {})
		       request)
	    response (handler request)]
	(if (contains? response :session)
	  (if (response :session)
	    ;;write-session and update date
	    (assoc-in response [:session :session_timestamp] (Date.)) 
	    ;;delete-session because response include {:session nil}
	    response) 
	  (if (empty? req-session)
	    response
	    (if expired?
	      ;;delete-session because session is expired
	      (assoc response :session nil)
	      ;;update date
	      (assoc response :session (assoc req-session :session_timestamp (Date.))))))))))
