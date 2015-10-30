var times = require('./fn').times;

function createGame(cols, rows, toWin) {
  return {
    rows: rows,
    cols: cols,
    pieces: times(cols, function (n) { return []; }),
    toWin: toWin || 4
  };
}

function hasColor(game, color, col, row) {
  return game[col] && game[col][row] === color;
}

function horizontal(game, col, row) {
  return {
    fwd: times(game.toWin, function (n) {
      return [col + n, row];
    }),

    bwd: times(game.toWin - 1, function (n) {
      return [col - n - 1, row];
    })
  };
}

function vertical(game, col, row) {
  return {
    fwd: [], // There's never pieces above the last one placed;
    bwd: times(game.toWin, function (n) {
      return [col, row - n];
    })
  };
}

function diagonalNE(game, col, row) {
  return {
    fwd: times(game.toWin, function (n) {
      return [col + n, row + n];
    }),

    bwd: times(game.toWin - 1, function (n) {
      return [col - n - 1, row - n - 1];
    })
  };
}

function diagonalSW(game, col, row) {
  return {
    fwd: times(game.toWin, function (n) {
      return [col - n, row + n];
    }),

    bwd: times(game.toWin - 1, function (n) {
      return [col + n + 1, row - n - 1];
    })
  };
}

function countContiguous(game, color, coords) {
  var c, r, count = 0;

  for (var i = 0; i < coords.length; i++) {
    c = coords[i][0];
    r = coords[i][1];
    if (game.pieces[c] && game.pieces[c][r] === color) {
      count += 1;
    } else {
      break;
    }
  }

  return count;
}

function check(game, col, row, color, getCoords) {
  var coords = getCoords(game, col, row);
  var contiguous = countContiguous(game, color, coords.fwd) +
        countContiguous(game, color, coords.bwd);
  return contiguous >= game.toWin;
}

function attemptWinning(game, color, col, row) {
  if (check(game, col, row, color, horizontal) ||
      check(game, col, row, color, vertical) ||
      check(game, col, row, color, diagonalNE) ||
      check(game, col, row, color, diagonalSW)) {
    console.log('YEP');
    game.winner = {player: color};
  }
}

function placePiece(game, col, color) {
  if (game.winner) {
    return;
  }

  if (col < 0 || col >= game.cols || game.pieces[col].length === game.rows) {
    throw new Error('Cannot place pieces outside game bounds');
  }

  game.pieces[col].push(color);
  attemptWinning(game, color, col, game.pieces[col].length - 1);
}

function play(cols, rows, players, currentPlayer) {
  var game = createGame(7, 7);
  currentPlayer = currentPlayer === undefined ? Math.floor(Math.random() * 2) : currentPlayer;

  return {
    rows: game.rows,
    cols: game.cols,
    pieces: game.pieces,

    getWinner: function () {
      return game.winner;
    },

    getCurrentPlayer: function() {
      return players[currentPlayer];
    },

    placePiece: function (col) {
      placePiece(game, col, players[currentPlayer]);
      currentPlayer = (currentPlayer + 1) % 2;
    }
  };
}

module.exports = {
  createGame: createGame,
  placePiece: placePiece,
  play: play
};
