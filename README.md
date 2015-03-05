# Hype Shuffle
Shuffling songs from various Hype Machine pages to facilitate the exploration of
new songs.

![Alt text](/screenshots/01.png?raw=true "Loading")
![Alt text](/screenshots/02.png?raw=true "Main screen")
![Alt text](/screenshots/03.png?raw=true "Lock screen")

## API
The API is built in Django and is deployed on Heroku at http://music.stevets.co/
of the form http://music.stevets.co/{name}/{mode}/{num}.

For example:
- Popular: http://music.stevets.co/popular/3day/1
- Latest: http://music.stevets.co/latest/all/1

## Android Client
The Android client shuffles and streams music from different pages. Eventually
I want to add collaborative filtering to the shuffle in order to play music that
the user would most likely be more interested in listening to.
