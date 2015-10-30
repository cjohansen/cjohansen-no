var react = require('react');
var dom = require('react-dom');
var div = react.DOM.div;

module.exports = function (events, el) {
  var Tile = function (tile) {
    return div({className: 'tile ' + tile.color},
               div({className: 'tile-inner'}));
  };

  var Col = react.createFactory(react.createClass({
    getInitialState: function () {
      return {active: false};
    },

    componentDidUpdate: function () {
      if (this.props.winner && this.state.active) {
        this.setState({active: false});
      }
    },

    render: function () {
      var comp = this;
      return div({
        className: 'col',
        onClick: function () {
          events.emit('placePiece', comp.props.idx);
        },
        onMouseEnter: function () {
          if (!comp.props.winner) {
            comp.setState({active: true});
          }
        },
        onMouseLeave: function () {
          comp.setState({active: false});
        }
      }, [
        this.state.active ? div({className: 'pending ' + this.props.player}) : null,
        this.props.tiles.map(Tile)
      ]);
    }
  }));

  var Winner = function (winner) {
    if (!winner) {
      return null;
    }
    return div({className: 'winner'},
               div({className: 'overlay'}),
               div({className: 'box'}, winner.player + ' wins!'));
  };

  var Board = function (game) {
    return div({className: 'board'},
               Winner(game.winner),
               game.pieces.map(function (tiles, idx) {
                 return Col({
                   idx: idx,
                   tiles: tiles,
                   player: game.player,
                   winner: game.winner
                 });
               }));
  };

  return function (game) {
    dom.render(Board(game), el);
  };
};
