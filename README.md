# SGSneakPeek
Small Implementation of the SeatGeek API

I used Retrofit to hit the access point, and RxAndroid to pull down and sort through the data.  Retrolambda helped cut down on the
verbosity.  I used Picasso for managing images, along with RxAndroid's caching to avoid repeated network calls after a state transition.
