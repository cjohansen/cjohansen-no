var times = require('./fn').times;

module.exports = function (game) {
  return {
    winner: game.getWinner(),
    player: game.getCurrentPlayer(),
    pieces: times(game.cols, function (col) {
      return times(game.rows, function (row) {
        var column = game.pieces[col];
        return {color: column && column[game.rows - row - 1]};
      });
    })
  };
};
