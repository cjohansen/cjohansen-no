var play = require('./src/game').play;
var createUI = require('./src/ui');
var prep = require('./src/prep');
var EventEmitter = require('events').EventEmitter;

var game = play(7, 7, ['yellow', 'red']);
var events = new EventEmitter();
var render = createUI(events, document.getElementById('app'));

events.on('placePiece', function (col) {
  game.placePiece(col);
  render(prep(game));
});

render(prep(game));
