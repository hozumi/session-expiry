# session-expiry

Session-expiry is a Clojure/Ring middleware that set session expiration.
Sessions are expired after a specified time from the last access.
Expired session is removed when request come. So **if request doesn't come expired sessions aren't removed**(Any solution?). Expired session is removed without changing session id.

## Usage

    (ns hello
      (:use [hozumi.session-expiry])
      (:use [ring.middleware.session]))

    (def app (-> handler
                 (wrap-session-expiry 3600) ;; 1 hour
                 wrap-session))

`(wrap-session-expiry handler expire-sec)`

## Installation
Leiningen
    [org.clojars.hozumi/session-expiry "1.0.0-SNAPSHOT"]
