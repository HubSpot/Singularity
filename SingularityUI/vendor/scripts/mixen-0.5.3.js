(function() {
  var Mixen, indexOf, moduleSuper, uniqueId,
    __slice = [].slice,
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  indexOf = function(haystack, needle) {
    var i, stalk, _i, _len;
    for (i = _i = 0, _len = haystack.length; _i < _len; i = ++_i) {
      stalk = haystack[i];
      if (stalk === needle) {
        return i;
      }
    }
    return -1;
  };

  uniqueId = (function() {
    var id;
    id = 0;
    return function() {
      return id++;
    };
  })();

  Mixen = function() {
    return Mixen.createMixen.apply(Mixen, arguments);
  };

  Mixen.createdMixens = {};

  Mixen.createMixen = function() {
    var Inst, Last, NewSuper, method, mods, module, _base, _i, _len, _ref, _ref1, _ref2;
    mods = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
    Last = mods[mods.length - 1];
    _ref = mods.slice(0).reverse();
    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
      module = _ref[_i];
      Inst = (function(_super) {
        __extends(Inst, _super);

        function Inst() {
          var args, mod, _j, _len1;
          args = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
          for (_j = 0, _len1 = mods.length; _j < _len1; _j++) {
            mod = mods[_j];
            mod.apply(this, args);
          }
        }

        return Inst;

      })(Last);
      Last = Inst;
      for (method in module.prototype) {
        Inst.prototype[method] = module.prototype[method];
      }
      _ref1 = module.prototype;
      for (method in _ref1) {
        if (!__hasProp.call(_ref1, method)) continue;
        if (method === 'constructor') {
          continue;
        }
        if (typeof module.prototype[method] !== 'function') {
          continue;
        }
        if (module.__super__ != null) {
          NewSuper = (function(_super) {
            __extends(NewSuper, _super);

            function NewSuper() {
              _ref2 = NewSuper.__super__.constructor.apply(this, arguments);
              return _ref2;
            }

            return NewSuper;

          })(module.__super__);
          module.__super__ = NewSuper;
        }
        if (module.__super__ == null) {
          module.__super__ = {};
        }
        if ((_base = module.__super__)[method] == null) {
          _base[method] = moduleSuper(module, method);
        }
      }
    }
    Last.prototype._mixen_id = uniqueId();
    Mixen.createdMixens[Last.prototype._mixen_id] = mods;
    return Last;
  };

  moduleSuper = function(module, method) {
    return function() {
      var args, current, id, modules, nextModule, pos;
      args = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
      current = this.constructor.prototype;
      id = null;
      while (true) {
        if (current === Object.prototype) {
          return;
        }
        id = current._mixen_id;
        if (id != null) {
          break;
        }
        current = current.constructor.__super__.constructor.prototype;
      }
      if (id == null) {
        return;
      }
      modules = Mixen.createdMixens[id];
      pos = indexOf(modules, module);
      nextModule = null;
      while (pos++ < modules.length - 1) {
        nextModule = modules[pos];
        if (nextModule.prototype[method] != null) {
          break;
        }
      }
      if ((nextModule != null) && (nextModule.prototype != null) && (nextModule.prototype[method] != null)) {
        return nextModule.prototype[method].apply(this, args);
      }
    };
  };

  if (typeof define === 'function' && define.amd) {
    define(function() {
      return Mixen;
    });
  } else if (typeof exports === 'object') {
    module.exports = Mixen;
  } else {
    window.Mixen = Mixen;
  }

}).call(this);
