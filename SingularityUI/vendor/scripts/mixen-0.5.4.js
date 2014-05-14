(function() {
  var Mixen, indexOf, moduleSuper, stack, uniqueId,
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

  stack = [];

  Mixen.createMixen = function() {
    var Inst, Last, NewSuper, k, method, mods, module, v, _base, _fn, _i, _len, _ref, _ref1, _ref2, _ref3;
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
      _ref1 = Inst.__super__;
      _fn = function(k, v, module) {
        return Inst.__super__[k] = function() {
          var args, ret;
          args = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
          stack.unshift(module);
          ret = v.apply(this, args);
          stack.shift();
          return ret;
        };
      };
      for (k in _ref1) {
        if (!__hasProp.call(_ref1, k)) continue;
        v = _ref1[k];
        if (typeof v !== 'function') {
          continue;
        }
        _fn(k, v, module);
      }
      Last = Inst;
      for (method in module.prototype) {
        Inst.prototype[method] = module.prototype[method];
      }
      _ref2 = module.prototype;
      for (method in _ref2) {
        if (!__hasProp.call(_ref2, method)) continue;
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
              _ref3 = NewSuper.__super__.constructor.apply(this, arguments);
              return _ref3;
            }

            return NewSuper;

          })(module.__super__);
          module.__super__ = NewSuper;
        } else {
          module.__super__ = {};
        }
        if ((_base = module.__super__)[method] == null) {
          _base[method] = moduleSuper(module, method);
        }
      }
    }
    Last._mixen_modules = mods;
    return Last;
  };

  moduleSuper = function(module, method) {
    return function() {
      var args, modules, nextModule, pos, _ref;
      args = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
      modules = ((_ref = stack[0]) != null ? _ref._mixen_modules : void 0) || this.constructor._mixen_modules;
      if (modules == null) {
        return;
      }
      pos = indexOf(modules, module);
      nextModule = null;
      if (pos === -1) {
        return;
      }
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
