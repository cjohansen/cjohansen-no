require('./helper');
var fiar = require('../src/game');
var createGame = fiar.createGame;
var placePiece = fiar.placePiece;
var play = fiar.play;

describe('Four-in-a-row', function () {
  describe('create game', function () {
    it('creates game', function () {
      var game = createGame(7, 7);

      assert.equals(game.rows, 7);
      assert.equals(game.cols, 7);
    });
  });

  describe('placePiece', function () {
    var game;

    beforeEach(function () {
      game = createGame(7, 7);
    });

    it('places piece of given color', function () {
      placePiece(game, 0, 'red');

      assert.equals(game.pieces[0][0], 'red');
    });

    it('places piece in last column', function () {
      placePiece(game, 6, 'red');

      assert.equals(game.pieces[6][0], 'red');
    });

    it('places pieces on top of each other', function () {
      placePiece(game, 6, 'red');
      placePiece(game, 6, 'yellow');

      assert.equals(game.pieces[6][0], 'red');
      assert.equals(game.pieces[6][1], 'yellow');
    });

    it('cannot place piece in full column', function () {
      placePiece(game, 6, 'red');
      placePiece(game, 6, 'yellow');
      placePiece(game, 6, 'red');
      placePiece(game, 6, 'yellow');
      placePiece(game, 6, 'red');
      placePiece(game, 6, 'yellow');
      placePiece(game, 6, 'red');

      assert.exception(function () {
        placePiece(game, 6, 'yellow');
      });
    });

    it('cannot place piece in non-existent columns', function () {
      assert.exception(function () {
        placePiece(game, -1, 'yellow');
      });

      assert.exception(function () {
        placePiece(game, 7, 'yellow');
      });
    });

    it('wins with four horizontal pieces', function () {
      placePiece(game, 3, 'red');
      placePiece(game, 2, 'red');
      placePiece(game, 1, 'red');
      placePiece(game, 0, 'red');

      assert.equals(game.winner, {player: 'red'});
    });

    it('wins with four horizontal pieces leftwards', function () {
      placePiece(game, 0, 'red');
      placePiece(game, 1, 'red');
      placePiece(game, 2, 'red');
      placePiece(game, 3, 'red');

      assert.equals(game.winner, {player: 'red'});
    });

    it('wins when completing four horizontal pieces', function () {
      placePiece(game, 2, 'red');
      placePiece(game, 3, 'red');
      placePiece(game, 0, 'red');
      placePiece(game, 1, 'red');

      assert.equals(game.winner, {player: 'red'});
    });

    it('wins with four vertical pieces', function () {
      placePiece(game, 0, 'red');
      placePiece(game, 0, 'red');
      placePiece(game, 0, 'red');
      placePiece(game, 0, 'red');

      assert.equals(game.winner, {player: 'red'});
    });

    it('wins with four north-east diagonal pieces', function () {
      placePiece(game, 0, 'red');
      placePiece(game, 1, 'yellow');
      placePiece(game, 1, 'red');
      placePiece(game, 2, 'yellow');
      placePiece(game, 2, 'yellow');
      placePiece(game, 2, 'red');
      placePiece(game, 3, 'yellow');
      placePiece(game, 3, 'red');
      placePiece(game, 3, 'yellow');
      placePiece(game, 3, 'red');

      assert.equals(game.winner, {player: 'red'});
    });

    it('wins with four south-west diagonal pieces', function () {
      placePiece(game, 3, 'yellow');
      placePiece(game, 3, 'red');
      placePiece(game, 3, 'yellow');
      placePiece(game, 3, 'red');
      placePiece(game, 2, 'yellow');
      placePiece(game, 2, 'yellow');
      placePiece(game, 2, 'red');
      placePiece(game, 1, 'yellow');
      placePiece(game, 1, 'red');
      placePiece(game, 0, 'red');

      assert.equals(game.winner, {player: 'red'});
    });

    it('wins with four north-west diagonal pieces', function () {
      placePiece(game, 0, 'red');
      placePiece(game, 0, 'yellow');
      placePiece(game, 0, 'red');
      placePiece(game, 0, 'yellow');
      placePiece(game, 1, 'red');
      placePiece(game, 1, 'red');
      placePiece(game, 1, 'yellow');
      placePiece(game, 2, 'red');
      placePiece(game, 2, 'yellow');
      placePiece(game, 3, 'yellow');

      assert.equals(game.winner, {player: 'yellow'});
    });

    it('wins with four wouth-east diagonal pieces', function () {
      placePiece(game, 3, 'yellow');
      placePiece(game, 2, 'red');
      placePiece(game, 2, 'yellow');
      placePiece(game, 1, 'red');
      placePiece(game, 1, 'red');
      placePiece(game, 1, 'yellow');
      placePiece(game, 0, 'red');
      placePiece(game, 0, 'yellow');
      placePiece(game, 0, 'red');
      placePiece(game, 0, 'yellow');

      assert.equals(game.winner, {player: 'yellow'});
    });

    it('cannot place pieces after a winner has been chosen', function () {
      placePiece(game, 0, 'red');
      placePiece(game, 1, 'red');
      placePiece(game, 2, 'red');
      placePiece(game, 3, 'red');
      placePiece(game, 4, 'red');

      refute.defined(game.pieces[4][0]);
    });
  });

  describe('play', function () {
    var game;

    beforeEach(function () {
      game = play(7, 7, ['red', 'yellow'], 0);
    });

    it('sets the initial player', function () {
      assert.equals(game.getCurrentPlayer(), 'red');
    });

    it('places a piece for the current player', function () {
      game.placePiece(0);

      assert.equals(game.pieces[0][0], 'red');
    });

    it('swaps player after placing piece', function () {
      game.placePiece(0);
      assert.equals(game.getCurrentPlayer(), 'yellow');

      game.placePiece(0);
      assert.equals(game.getCurrentPlayer(), 'red');

      assert.equals(game.pieces[0][0], 'red');
      assert.equals(game.pieces[0][1], 'yellow');
    });
  });
});
