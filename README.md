# stechuhr

Rapidly built prototype for simple task tracking and written in Clojure and Clojurescript powered by [replikativ](http://replikativ.io].

A recorded screencast can be found [here](https://youtu.be/LW8v6Cr9BcM)

## Usage
Make sure [leiningen](https://leiningen.org/) is installed:

Build the web app:
```
lein cljsbuild once
```

Start the server:
```
lein run
```

Open the web app [http://localhost:8080/index.html](http://localhost:8080/index.html)

## Development

If you want to work with the code and want real-time development:

Start the server:
```
lein run
```

Start figwheel:
```
lein figwheel
```

Open `src/cljs/core.cljs` in your favourite editor and see changes at [http://localhost:3449](http://localhost:3449).

## License

Copyright © 2017 Konrad Kühne

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
