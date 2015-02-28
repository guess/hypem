from time import time
import urllib2
import urllib
from bs4 import BeautifulSoup
import json
from django.http import HttpResponse
from hypem.settings import DEBUG


BASE_URL = "http://hypem.com/"


def load_page(url):
    """ Return the HTML and cookie at the specified HypeMachine URL.
        Throws a URLError if there is a problem loading the page.
    """
    # Format the URL to what Hype Machine requires
    # Hype Machine requires the following query after the URL
    # Example: /popular/3day/3?ax=1&ts=1424219886.61
    data = {'ax':1 , 'ts': time()}
    data_encoded = urllib.urlencode(data)
    complete_url = url + "?{}".format(data_encoded)

    # Load the page
    request = urllib2.Request(complete_url)
    response = urllib2.urlopen(request)         # TODO: Could throw URLError
    # Get the cookie in order to get song URLs
    cookie = response.headers.get('Set-Cookie')
    html = response.read()
    response.close()
    return html, cookie


def parse_page(html):
    """ Parse a Hype Machine HTML page and return the page's JSON data.
        Return None if the HypeMachine's JSON is an invalid form.

        The page's JSON data will be of the following form:
        {
            "page_arg":     "",
            "page_cur":     "/popular/3day/3?ax=1&ts=1424219886.61",
            "page_mode":    "3day",
            "page_name":    "popular",
            "page_num":     "3",
            "page_prev":    "/popular/3day/2",
            "page_sort":    "",
            "title":        "Popular MP3 & Music Blog Tracks / Hype Machine",
            "tracks": [
                {
                    "artist":   "Pat Lok feat. Desir\u00e9e Dawson",
                    "fav":      0,
                    "id":       "29dgt",
                    "is_bc":    false,
                    "is_sc":    true,
                    "key":      "3add9a261b27df4a3f01163303a5fd5c",
                    "postid":   2623620,
                    "posturl":  "http://acidstag.com/2015/02/16/non-mixtape-mixes-volume-61/",
                    "song":     "All In My Head (Howson's Groove Remix)",
                    "time":     330,
                    "ts":       1424048450,
                    "type":     "normal"
                },
                ...
            ]
        }
    """
    soup = BeautifulSoup(html)
    page_data = soup.find(id="displayList-data")
    try:
        page_data = json.loads(page_data.text)
        if DEBUG:
            print json.dumps(page_data, sort_keys=True,indent=4, separators=(',', ': '))
        return page_data
    except ValueError:
        print "HypeMachine contained invalid JSON."
        return None


def get_song_url(track_id, track_key, track_type, cookie):
    """ Get the URL of a Hype Machine song with ID track_id.
        Return None if an error occurs. """
    url = None  # Default value if cannot find song URL
    if track_type is False:
        return None
    try:
        serve_url = "http://hypem.com/serve/source/{}/{}".format(track_id, track_key)
        request = urllib2.Request(serve_url, "", {'Content-Type': 'application/json'})
        request.add_header('cookie', cookie)
        response = urllib2.urlopen(request)
        song_data_json = response.read()
        response.close()
        song_data = json.loads(song_data_json)
        url = song_data[u'url']
    except urllib2.HTTPError, e:
        print 'HTTPError = ' + str(e.code) + " trying hypem download url."
    except urllib2.URLError, e:
        print 'URLError = ' + str(e.reason)  + " trying hypem download url."
    except Exception, e:
        print 'Exception: ' + str(e)
    return url


def get_tracks_metadata(url):
    """ Get metadata for songs at the specified URL.
        Return a JSON array of tracks of the following form:
        TODO:
    """
    try:
        request = urllib2.Request(url, "" , {'Content-Type': 'application/json'})
        response = urllib2.urlopen(request)
        song_data_json = response.read()
        response.close()
        tracks_list = json.loads(song_data_json)
        tracks_list.pop("version")
    except urllib2.HTTPError, e:
        print 'HTTPError = ' + str(e.code) + " trying hypem download url."
        tracks_list = []
    except urllib2.URLError, e:
        print 'URLError = ' + str(e.reason)  + " trying hypem download url."
        tracks_list = []
    except Exception, e:
        print 'Exception: ' + str(e)
        tracks_list = []
    return tracks_list


def download(name, mode, num, sort=None):
    url = BASE_URL + "{}/{}/{}".format(name, mode, num)
    html, cookie = load_page(url)
    page_data = parse_page(html)
    track_list = page_data[u'tracks']

    # Get additional track metadata
    serve_url = "http://hypem.com/playlist/{}/{}/json/{}/data.js".format(name, mode, num)
    metadata = get_tracks_metadata(serve_url)
    print metadata

    tracks = []
    for track in range(0, len(metadata)):
        try:
            # Create a new song
            song = {}

            # Update song's metadata
            song['artist'] = metadata[str(track)].get(u'artist', None)
            song['title'] = metadata[str(track)].get(u'title', None)
            song['date_posted'] = metadata[str(track)].get(u'dateposted', None)
            song['loved_count'] = metadata[str(track)].get(u'loved_count', None)
            song['time'] = metadata[str(track)].get(u'time', None)
            song['thumb_url'] = metadata[str(track)].get(u'thumb_url', None)
            song['thumb_url_medium'] = metadata[str(track)].get(u'thumb_url_medium', None)
            song['thumb_url_large'] = metadata[str(track)].get(u'thumb_url_large', None)
            song['thumb_url_artist'] = metadata[str(track)].get(u'thumb_url_artist', None)
            song['posted_count'] = metadata[str(track)].get(u'posted_count', None)
            song['post_url'] = metadata[str(track)].get(u'posturl', None)

            # Get the URL of the song
            song_url = get_song_url(
                track_list[track][u'id'],
                track_list[track][u'key'],
                track_list[track][u'type'],
                cookie
            )
            song['url'] = song_url
            tracks.append(song)

        except Exception, e:
            print 'Exception: ' + str(e)
            continue

    return tracks



def home(request, name, mode, num, sort=None):
    tracks = download(name, mode, num, sort)
    data = json.dumps(tracks)
    return HttpResponse(data, content_type="application/json")
