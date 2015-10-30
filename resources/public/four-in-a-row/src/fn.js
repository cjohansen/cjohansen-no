exports.times = function (n, callback) {
  var res = [];
  for (var i = 0; i < n; i++) {
    res.push(callback(i));
  }
  return res;
};
