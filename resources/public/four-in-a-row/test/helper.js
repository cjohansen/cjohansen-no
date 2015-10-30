var referee = require('referee');
var formatio = require('formatio');

referee.format = formatio.ascii;

global.assert = referee.assert;
global.refute = referee.refute;
