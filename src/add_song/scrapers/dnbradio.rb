# Webdriver script for scraping dnbradio.com currently playing song
require 'watir-webdriver'

begin
  b = Watir::Browser.new
  b.goto 'http://dnbradio.com/embed/live'

  artist = b.span(:id, "artist").when_present.text
  title = b.span(:id, "title").when_present.text

  response = '{:artist "' + artist + '" :title "' + title + '"}'
  b.close
  
  puts response
  exit 0
rescue Exception => e
  puts e.message
  puts e.backtrace.inspect
  exit 1
end
