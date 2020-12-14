# clojure-add-song

Clojure CLI app scraping currently playing online radio song and adding it to Spotify playlist. Learning project, Clojure was not too suitable for this kind of stuff.

Has scrapers for some SomaFM.com channels using enlive and DnBRadio.com using Ruby watir (for the sake of learning how to do subprocess calls).

## todo

- [x] Passing command line arguments
- [x] Scraping
  - [x] Using Enlive
  - [x] Using webdriver (call ruby script with watir-webdriver)
- [x] Spotify Web API authentication (authorization code login / OAuth)

## Resources / Bookmarks on topic

http://diigo.com/user/jasalt/clojure
