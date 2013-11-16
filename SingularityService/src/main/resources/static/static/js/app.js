(function(/*! Brunch !*/) {
  'use strict';

  var globals = typeof window !== 'undefined' ? window : global;
  if (typeof globals.require === 'function') return;

  var modules = {};
  var cache = {};

  var has = function(object, name) {
    return ({}).hasOwnProperty.call(object, name);
  };

  var expand = function(root, name) {
    var results = [], parts, part;
    if (/^\.\.?(\/|$)/.test(name)) {
      parts = [root, name].join('/').split('/');
    } else {
      parts = name.split('/');
    }
    for (var i = 0, length = parts.length; i < length; i++) {
      part = parts[i];
      if (part === '..') {
        results.pop();
      } else if (part !== '.' && part !== '') {
        results.push(part);
      }
    }
    return results.join('/');
  };

  var dirname = function(path) {
    return path.split('/').slice(0, -1).join('/');
  };

  var localRequire = function(path) {
    return function(name) {
      var dir = dirname(path);
      var absolute = expand(dir, name);
      return globals.require(absolute, path);
    };
  };

  var initModule = function(name, definition) {
    var module = {id: name, exports: {}};
    cache[name] = module;
    definition(module.exports, localRequire(name), module);
    return module.exports;
  };

  var require = function(name, loaderPath) {
    var path = expand(name, '.');
    if (loaderPath == null) loaderPath = '/';

    if (has(cache, path)) return cache[path].exports;
    if (has(modules, path)) return initModule(path, modules[path]);

    var dirIndex = expand(path, './index');
    if (has(cache, dirIndex)) return cache[dirIndex].exports;
    if (has(modules, dirIndex)) return initModule(dirIndex, modules[dirIndex]);

    throw new Error('Cannot find module "' + name + '" from '+ '"' + loaderPath + '"');
  };

  var define = function(bundle, fn) {
    if (typeof bundle === 'object') {
      for (var key in bundle) {
        if (has(bundle, key)) {
          modules[key] = bundle[key];
        }
      }
    } else {
      modules[bundle] = fn;
    }
  };

  var list = function() {
    var result = [];
    for (var item in modules) {
      if (has(modules, item)) {
        result.push(item);
      }
    }
    return result;
  };

  globals.require = require;
  globals.require.define = define;
  globals.require.register = define;
  globals.require.list = list;
  globals.require.brunch = true;
})();
require.register("application", function(exports, require, module) {
var Application, Requests, Router, State, TasksActive, TasksScheduled,
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };

Router = require('lib/router');

State = require('models/State');

Requests = require('collections/Requests');

TasksActive = require('collections/TasksActive');

TasksScheduled = require('collections/TasksScheduled');

Application = (function() {
  function Application() {
    this.fetchResources = __bind(this.fetchResources, this);
    this.initialize = __bind(this.initialize, this);
  }

  Application.prototype.initialize = function() {
    var _this = this;
    this.views = {};
    this.collections = {};
    return this.fetchResources(function() {
      $('.page-loader.fixed').hide();
      _this.router = new Router;
      Backbone.history.start({
        pushState: false,
        root: '/singularity/'
      });
      return typeof Object.freeze === "function" ? Object.freeze(_this) : void 0;
    });
  };

  Application.prototype.fetchResources = function(success) {
    var resolve, resources,
      _this = this;
    this.resolve_countdown = 0;
    resolve = function() {
      _this.resolve_countdown -= 1;
      if (_this.resolve_countdown === 0) {
        return success();
      }
    };
    this.resolve_countdown += 1;
    this.state = new State;
    this.state.fetch({
      error: function() {
        return vex.dialog.alert('An error occurred while trying to load the Singularity state.');
      },
      success: function() {
        return resolve();
      }
    });
    resources = [
      {
        collection_key: 'requests',
        collection: Requests,
        error_phrase: 'requests'
      }, {
        collection_key: 'tasksActive',
        collection: TasksActive,
        error_phrase: 'active tasks'
      }, {
        collection_key: 'tasksScheduled',
        collection: TasksScheduled,
        error_phrase: 'scheduled tasks'
      }
    ];
    return _.each(resources, function(r) {
      _this.resolve_countdown += 1;
      _this.collections[r.collection_key] = new r.collection;
      return _this.collections[r.collection_key].fetch({
        error: function() {
          return vex.dialog.alert("An error occurred while trying to load Singularity " + r.error_phrase + ".");
        },
        success: function() {
          return resolve();
        }
      });
    });
  };

  return Application;

})();

module.exports = new Application;
});

;require.register("collections/Requests", function(exports, require, module) {
var Collection, Requests, _ref,
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

Collection = require('./collection');

Requests = (function(_super) {
  __extends(Requests, _super);

  function Requests() {
    _ref = Requests.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  Requests.prototype.url = "http://" + env.SINGULARITY_BASE + "/" + constants.api_base + "/requests";

  Requests.prototype.parse = function(requests) {
    var _this = this;
    _.each(requests, function(request, i) {
      request.id = request.id;
      request.deployUser = _this.parseDeployUser(request);
      request.JSONString = utils.stringJSON(request);
      request.timestampHuman = moment(request.timestamp).from();
      return requests[i] = request;
    });
    return requests;
  };

  Requests.prototype.parseDeployUser = function(request) {
    var _ref1, _ref2, _ref3;
    return ((_ref1 = (_ref2 = request.executorData) != null ? (_ref3 = _ref2.env) != null ? _ref3.DEPLOY_USER : void 0 : void 0) != null ? _ref1 : '').split('@')[0];
  };

  Requests.prototype.comparator = 'name';

  return Requests;

})(Collection);

module.exports = Requests;
});

;require.register("collections/Tasks", function(exports, require, module) {
var Collection, Tasks, _ref,
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

Collection = require('./collection');

Tasks = (function(_super) {
  __extends(Tasks, _super);

  function Tasks() {
    _ref = Tasks.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  Tasks.prototype.comparator = 'name';

  return Tasks;

})(Collection);

module.exports = Tasks;
});

;require.register("collections/TasksActive", function(exports, require, module) {
var Collection, Tasks, TasksActive, _ref,
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

Collection = require('./collection');

Tasks = require('./Tasks');

TasksActive = (function(_super) {
  __extends(TasksActive, _super);

  function TasksActive() {
    _ref = TasksActive.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  TasksActive.prototype.url = "http://" + env.SINGULARITY_BASE + "/" + constants.api_base + "/tasks/active";

  TasksActive.prototype.parse = function(tasks) {
    var _this = this;
    _.each(tasks, function(task, i) {
      var _ref1;
      task.id = task.task.taskId.value;
      task.name = task.task.name;
      task.resources = task.taskRequest.request.resources;
      task.host = (_ref1 = task.offer.hostname) != null ? _ref1.split('.')[0] : void 0;
      task.startedAt = task.taskId.startedAt;
      task.startedAtHuman = moment(task.taskId.startedAt).from();
      task.JSONString = utils.stringJSON(task);
      return tasks[i] = task;
    });
    return tasks;
  };

  TasksActive.prototype.comparator = 'startedAt';

  return TasksActive;

})(Tasks);

module.exports = TasksActive;
});

;require.register("collections/TasksScheduled", function(exports, require, module) {
var Collection, Tasks, TasksScheduled, _ref,
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

Collection = require('./collection');

Tasks = require('./Tasks');

TasksScheduled = (function(_super) {
  __extends(TasksScheduled, _super);

  function TasksScheduled() {
    _ref = TasksScheduled.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  TasksScheduled.prototype.url = "http://" + env.SINGULARITY_BASE + "/" + constants.api_base + "/tasks/scheduled";

  TasksScheduled.prototype.parse = function(tasks) {
    var _this = this;
    _.each(tasks, function(task, i) {
      task.id = _this.parsePendingId(task.pendingTaskId);
      task.name = task.id;
      task.nextRunAt = task.pendingTaskId.nextRunAt;
      task.nextRunAtHuman = moment(task.nextRunAt).fromNow();
      task.schedule = task.request.schedule;
      task.JSONString = utils.stringJSON(task);
      return tasks[i] = task;
    });
    return tasks;
  };

  TasksScheduled.prototype.parsePendingId = function(pendingTaskId) {
    return "" + pendingTaskId.requestId + "-" + pendingTaskId.nextRunAt + "-" + pendingTaskId.instanceNo;
  };

  TasksScheduled.prototype.comparator = 'nextRunAt';

  return TasksScheduled;

})(Tasks);

module.exports = TasksScheduled;
});

;require.register("collections/collection", function(exports, require, module) {
var Collection, _ref,
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

module.exports = Collection = (function(_super) {
  __extends(Collection, _super);

  function Collection() {
    _ref = Collection.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  return Collection;

})(Backbone.Collection);
});

;require.register("constants", function(exports, require, module) {
var constants;

constants = {
  app_name: 'singularity',
  config_server_base: '/singularity',
  api_base: 'singularity/v1'
};

module.exports = constants;
});

;require.register("env", function(exports, require, module) {
var env;

if (window.location.hostname === 'localhost') {
  env = {
    env: 'local',
    SINGULARITY_BASE: 'http://heliograph.iad01.hubspot-networks.net:7005'
  };
} else {
  env = {
    env: 'prod',
    SINGULARITY_BASE: ''
  };
}

module.exports = env;
});

;require.register("initialize", function(exports, require, module) {
window.env = require('env');

window.utils = require('utils');

window.constants = require('constants');

window.app = require('application');

_.mixin(_.string.exports());

vex.defaultOptions.className = 'vex-theme-default';

$(function() {
  return app.initialize();
});
});

;require.register("lib/login", function(exports, require, module) {

});

;require.register("lib/router", function(exports, require, module) {
var DashboardView, NavigationView, PageNotFoundView, RequestView, RequestsView, Router, TaskView, TasksView, nav, _ref,
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

DashboardView = require('views/dashboard');

RequestsView = require('views/requests');

RequestView = require('views/request');

TasksView = require('views/tasks');

TaskView = require('views/task');

PageNotFoundView = require('views/page_not_found');

NavigationView = require('views/navigation');

nav = function() {
  if (app.views.navigationView == null) {
    app.views.navigationView = new NavigationView;
  }
  return app.views.navigationView.render();
};

Router = (function(_super) {
  __extends(Router, _super);

  function Router() {
    _ref = Router.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  Router.prototype.routes = {
    '(/)': 'dashboard',
    'requests(/)': 'requests',
    'request/:requestId': 'request',
    'tasks(/)': 'tasks',
    'task/:taskId': 'task',
    '*anything': 'templateFromURLFragment'
  };

  Router.prototype.dashboard = function() {
    nav();
    if (app.views.dashboard == null) {
      app.views.dashboard = new DashboardView;
    }
    app.views.current = app.views.dashboard;
    return app.views.dashboard.render();
  };

  Router.prototype.requests = function() {
    nav();
    if (app.views.requests == null) {
      app.views.requests = new RequestsView;
    }
    app.views.current = app.views.requests;
    return app.views.requests.render();
  };

  Router.prototype.request = function(requestId) {
    nav();
    if (!app.views.requestViews) {
      app.views.requestViews = {};
    }
    if (!app.views.requestViews[requestId]) {
      app.views.requestViews[requestId] = new RequestView({
        requestId: requestId
      });
    }
    app.views.current = app.views.requestViews[requestId];
    return app.views.requestViews[requestId].render();
  };

  Router.prototype.tasks = function() {
    nav();
    if (app.views.tasks == null) {
      app.views.tasks = new TasksView;
    }
    app.views.current = app.views.tasks;
    return app.views.tasks.render();
  };

  Router.prototype.task = function(taskId) {
    nav();
    if (!app.views.taskViews) {
      app.views.taskViews = {};
    }
    if (!app.views.taskViews[taskId]) {
      app.views.taskViews[taskId] = new TaskView({
        taskId: taskId
      });
    }
    app.views.current = app.views.taskViews[taskId];
    return app.views.taskViews[taskId].render();
  };

  Router.prototype.templateFromURLFragment = function() {
    var error, template;
    nav();
    app.views.current = void 0;
    template = void 0;
    try {
      template = require("../views/templates/" + Backbone.history.fragment);
    } catch (_error) {
      error = _error;
    }
    if (template) {
      $('body > .app').html(template);
      return;
    }
    return this.show404();
  };

  Router.prototype.show404 = function() {
    nav();
    if (app.views.pageNotFound == null) {
      app.views.pageNotFound = new PageNotFoundView;
    }
    app.views.current = app.views.pageNotFound;
    return app.views.pageNotFound.render();
  };

  return Router;

})(Backbone.Router);

module.exports = Router;
});

;require.register("lib/view_helper", function(exports, require, module) {
Handlebars.registerHelper('ifLT', function(v1, v2, options) {
  if (v1 < v2) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }
});

Handlebars.registerHelper('ifGT', function(v1, v2, options) {
  if (v1 > v2) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }
});

Handlebars.registerHelper('pluralize', function(number, single, plural) {
  if (number === 1) {
    return single;
  } else {
    return plural;
  }
});

Handlebars.registerHelper('hardBreak', function(string, options) {
  return string.replace(/(:|-)/g, '$1<wbr/>');
});

Handlebars.registerHelper('eachWithFn', function(items, options) {
  var _this = this;
  return _(items).map(function(item, i, items) {
    item._counter = i;
    item._1counter = i + 1;
    item._first = i === 0 ? true : false;
    item._last = i === (items.length - 1) ? true : false;
    item._even = (i + 1) % 2 === 0 ? true : false;
    item._thirded = (i + 1) % 3 === 0 ? true : false;
    item._sixthed = (i + 1) % 6 === 0 ? true : false;
    _.isFunction(options.hash.fn) && options.hash.fn.apply(options, [item, i, items]);
    return options.fn(item);
  }).join('');
});
});

;require.register("models/RequestTasks", function(exports, require, module) {
var Model, RequestTasks, _ref,
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

Model = require('./model');

RequestTasks = (function(_super) {
  __extends(RequestTasks, _super);

  function RequestTasks() {
    this.initialize = __bind(this.initialize, this);
    this.url = __bind(this.url, this);
    _ref = RequestTasks.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  RequestTasks.prototype.url = function() {
    return "http://" + env.SINGULARITY_BASE + "/" + constants.api_base + "/history/request/" + this.requestId + "/tasks";
  };

  RequestTasks.prototype.initialize = function() {
    this.requestId = this.attributes.requestId;
    return delete this.attributes.requestId;
  };

  RequestTasks.prototype.parse = function(tasks) {
    _.each(tasks, function(task) {
      task.id = "" + task.requestId + "-" + task.startedAt + "-" + task.instanceNo + "-" + task.rackId;
      return task.name = task.id;
    });
    return tasks;
  };

  return RequestTasks;

})(Model);

module.exports = RequestTasks;
});

;require.register("models/State", function(exports, require, module) {
var Model, State, _ref,
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

Model = require('./model');

State = (function(_super) {
  __extends(State, _super);

  function State() {
    this.parse = __bind(this.parse, this);
    _ref = State.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  State.prototype.url = function() {
    return "http://" + env.SINGULARITY_BASE + "/" + constants.api_base + "/state";
  };

  State.prototype.parse = function(state) {
    state.uptimeHuman = moment.duration(state.uptime).humanize();
    return state;
  };

  return State;

})(Model);

module.exports = State;
});

;require.register("models/Task", function(exports, require, module) {
var Model, Task, _ref,
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

Model = require('./model');

Task = (function(_super) {
  __extends(Task, _super);

  function Task() {
    _ref = Task.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  Task.prototype.url = function() {
    return "http://" + env.SINGULARITY_BASE + "/" + constants.api_base + "/task/" + (this.get('name'));
  };

  return Task;

})(Model);

module.exports = Task;
});

;require.register("models/model", function(exports, require, module) {
var Model, _ref,
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

module.exports = Model = (function(_super) {
  __extends(Model, _super);

  function Model() {
    _ref = Model.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  return Model;

})(Backbone.Model);
});

;require.register("utils", function(exports, require, module) {
var Utils;

Utils = (function() {
  function Utils() {}

  Utils.prototype.getHTMLTitleFromHistoryFragment = function(fragment) {
    return _.capitalize(fragment.split('\/').join(' '));
  };

  Utils.prototype.stringJSON = function(object) {
    return JSON.stringify(object, null, '    ');
  };

  Utils.prototype.viewJSON = function(object) {
    return vex.dialog.alert({
      contentCSS: {
        width: 800
      },
      message: "<pre>" + (utils.stringJSON(object)) + "</pre>"
    });
  };

  Utils.prototype.getAcrossCollections = function(collections, id) {
    var model;
    model = void 0;
    _.each(collections, function(collection) {
      var _ref;
      return model = (_ref = collection.get(id)) != null ? _ref : model;
    });
    return model;
  };

  return Utils;

})();

module.exports = new Utils;
});

;require.register("views/dashboard", function(exports, require, module) {
var DashboardView, View, _ref,
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

View = require('./view');

DashboardView = (function(_super) {
  __extends(DashboardView, _super);

  function DashboardView() {
    this.render = __bind(this.render, this);
    _ref = DashboardView.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  DashboardView.prototype.template = require('./templates/dashboard');

  DashboardView.prototype.render = function() {
    return this.$el.html(this.template({
      state: app.state.toJSON()
    }));
  };

  return DashboardView;

})(View);

module.exports = DashboardView;
});

;require.register("views/navigation", function(exports, require, module) {
var NavigationView, View, _ref,
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

View = require('./view');

NavigationView = (function(_super) {
  __extends(NavigationView, _super);

  function NavigationView() {
    this.collapse = __bind(this.collapse, this);
    this.renderTheme = __bind(this.renderTheme, this);
    this.renderNavLinks = __bind(this.renderNavLinks, this);
    this.renderTitle = __bind(this.renderTitle, this);
    this.render = __bind(this.render, this);
    this.initialize = __bind(this.initialize, this);
    _ref = NavigationView.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  NavigationView.prototype.el = '#top-level-nav';

  NavigationView.prototype.initialize = function() {
    $('#top-level-nav').dblclick(function() {
      return window.scrollTo(0, 0);
    });
    return this.theme = 'light';
  };

  NavigationView.prototype.render = function() {
    this.renderTitle();
    this.renderNavLinks();
    return this.collapse();
  };

  NavigationView.prototype.renderTitle = function() {
    var subtitle;
    subtitle = utils.getHTMLTitleFromHistoryFragment(Backbone.history.fragment);
    if (subtitle !== '') {
      subtitle = ' — ' + subtitle;
    }
    return $('head title').text("Singularity" + subtitle);
  };

  NavigationView.prototype.renderNavLinks = function() {
    var $anchors, $nav;
    $nav = this.$el;
    this.renderTheme(this.theme);
    $anchors = $nav.find('ul.nav a:not(".dont-route")');
    $anchors.each(function() {
      var route;
      route = $(this).data('href');
      return $(this).attr('href', "/" + constants.app_name + "/" + route).data('route', route);
    });
    $nav.find('li').removeClass('active');
    return $anchors.each(function() {
      if ($(this).attr('href') === ("/" + constants.app_name + "/" + Backbone.history.fragment)) {
        return $(this).parents('li').addClass('active');
      }
    });
  };

  NavigationView.prototype.renderTheme = function(theme) {
    var previous_theme;
    previous_theme = this.theme === 'light' ? 'dark' : 'light';
    $('html').addClass("" + theme + "strap").removeClass("" + previous_theme + "strap");
    return $('#theme-changer').html(_.capitalize(previous_theme)).unbind('click').click(function() {
      var new_theme;
      new_theme = this.theme === 'dark' ? 'light' : 'dark';
      return this.theme = new_theme;
    });
  };

  NavigationView.prototype.collapse = function() {
    var $collapse;
    $collapse = $('#top-level-nav .nav-collapse');
    if ($collapse.data().collapse) {
      return $collapse.collapse('hide');
    }
  };

  return NavigationView;

})(View);

module.exports = NavigationView;
});

;require.register("views/page_not_found", function(exports, require, module) {
var PageNotFoundView, View, _ref,
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

View = require('./view');

PageNotFoundView = (function(_super) {
  __extends(PageNotFoundView, _super);

  function PageNotFoundView() {
    this.render = __bind(this.render, this);
    _ref = PageNotFoundView.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  PageNotFoundView.prototype.template = require('./templates/404');

  PageNotFoundView.prototype.render = function() {
    return $(this.el).html(this.template);
  };

  return PageNotFoundView;

})(View);

module.exports = PageNotFoundView;
});

;require.register("views/request", function(exports, require, module) {
var RequestTasks, RequestView, View, _ref,
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

View = require('./view');

RequestTasks = require('../models/RequestTasks');

RequestView = (function(_super) {
  __extends(RequestView, _super);

  function RequestView() {
    this.render = __bind(this.render, this);
    this.initialize = __bind(this.initialize, this);
    _ref = RequestView.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  RequestView.prototype.template = require('./templates/request');

  RequestView.prototype.initialize = function() {
    var _this = this;
    this.request = app.collections.requests.get(this.options.requestId);
    this.requestTasks = new RequestTasks({
      requestId: this.options.requestId
    });
    return this.requestTasks.fetch().done(function() {
      return _this.render();
    });
  };

  RequestView.prototype.render = function() {
    var context;
    if (!this.request) {
      return false;
    }
    context = {
      request: this.request.toJSON(),
      requestTasks: this.requestTasks.toJSON()
    };
    this.$el.html(this.template(context));
    return this.setupEvents();
  };

  RequestView.prototype.setupEvents = function() {
    return this.$el.find('.view-json').unbind('click').click(function(event) {
      var _ref1;
      return utils.viewJSON((_ref1 = utils.getAcrossCollections([app.collections.tasksActive, app.collections.tasksScheduled], $(event.target).data('task-id'))) != null ? _ref1.toJSON() : void 0);
    });
  };

  return RequestView;

})(View);

module.exports = RequestView;
});

;require.register("views/requests", function(exports, require, module) {
var RequestsView, View, _ref,
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

View = require('./view');

RequestsView = (function(_super) {
  __extends(RequestsView, _super);

  function RequestsView() {
    this.setUpSearchEvents = __bind(this.setUpSearchEvents, this);
    this.render = __bind(this.render, this);
    _ref = RequestsView.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  RequestsView.prototype.template = require('./templates/requests');

  RequestsView.prototype.render = function() {
    var context;
    context = {
      requests: app.collections.requests.toJSON()
    };
    this.$el.html(this.template(context));
    this.setupEvents();
    return this.setUpSearchEvents();
  };

  RequestsView.prototype.setupEvents = function() {
    return this.$el.find('.view-json').unbind('click').click(function(event) {
      return utils.viewJSON((app.collections.requests.get($(event.target).data('request-id'))).toJSON());
    });
  };

  RequestsView.prototype.setUpSearchEvents = function() {
    var $rows, $search, lastText,
      _this = this;
    $search = this.$el.find('input[type="search"]').focus();
    $rows = this.$el.find('tbody > tr');
    lastText = _.trim($search.val());
    return $search.on('change keypress paste focus textInput input click keydown', function() {
      var text;
      text = _.trim($search.val());
      if (text === '') {
        $rows.removeClass('filtered');
      }
      if (text !== lastText) {
        return $rows.each(function() {
          var $row;
          $row = $(this);
          if (!_.string.contains($row.data('request-id').toLowerCase(), text.toLowerCase())) {
            return $row.addClass('filtered');
          } else {
            return $row.removeClass('filtered');
          }
        });
      }
    });
  };

  return RequestsView;

})(View);

module.exports = RequestsView;
});

;require.register("views/task", function(exports, require, module) {
var TaskView, View, _ref,
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

View = require('./view');

TaskView = (function(_super) {
  __extends(TaskView, _super);

  function TaskView() {
    this.render = __bind(this.render, this);
    this.initialize = __bind(this.initialize, this);
    _ref = TaskView.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  TaskView.prototype.template = require('./templates/task');

  TaskView.prototype.initialize = function() {
    return this.task = utils.getAcrossCollections([app.collections.tasksActive, app.collections.tasksScheduled], this.options.taskId);
  };

  TaskView.prototype.render = function() {
    var context;
    if (!this.task) {
      vex.dialog.alert('Could not open a task by that ID. Ask <b>@wsorenson</b>...');
      return;
    }
    context = {
      task: this.task.toJSON()
    };
    return this.$el.html(this.template(context));
  };

  return TaskView;

})(View);

module.exports = TaskView;
});

;require.register("views/tasks", function(exports, require, module) {
var TasksView, View, _ref,
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

View = require('./view');

TasksView = (function(_super) {
  __extends(TasksView, _super);

  function TasksView() {
    this.setUpSearchEvents = __bind(this.setUpSearchEvents, this);
    this.render = __bind(this.render, this);
    _ref = TasksView.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  TasksView.prototype.template = require('./templates/tasks');

  TasksView.prototype.render = function() {
    var context;
    context = {
      tasksActive: app.collections.tasksActive.sort().toJSON().reverse(),
      tasksScheduled: app.collections.tasksScheduled.sort().toJSON().reverse()
    };
    this.$el.html(this.template(context));
    this.setupEvents();
    return this.setUpSearchEvents();
  };

  TasksView.prototype.setupEvents = function() {
    return this.$el.find('.view-json').unbind('click').click(function(event) {
      var _ref1;
      return utils.viewJSON((_ref1 = utils.getAcrossCollections([app.collections.tasksActive, app.collections.tasksScheduled], $(event.target).data('task-id'))) != null ? _ref1.toJSON() : void 0);
    });
  };

  TasksView.prototype.setUpSearchEvents = function() {
    var $rows, $search, lastText,
      _this = this;
    $search = this.$el.find('input[type="search"]').focus();
    $rows = this.$el.find('tbody > tr');
    lastText = _.trim($search.val());
    return $search.on('change keypress paste focus textInput input click keydown', function() {
      var text;
      text = _.trim($search.val());
      if (text === '') {
        $rows.removeClass('filtered');
      }
      if (text !== lastText) {
        return $rows.each(function() {
          var $row;
          $row = $(this);
          if (!_.string.contains($row.data('task-id').toLowerCase(), text.toLowerCase())) {
            return $row.addClass('filtered');
          } else {
            return $row.removeClass('filtered');
          }
        });
      }
    });
  };

  return TasksView;

})(View);

module.exports = TasksView;
});

;require.register("views/templates/404", function(exports, require, module) {
var __templateData = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  


  return "<h1>Page not found</h1>";
  });
if (typeof define === 'function' && define.amd) {
  define([], function() {
    return __templateData;
  });
} else if (typeof module === 'object' && module && module.exports) {
  module.exports = __templateData;
} else {
  __templateData;
}
});

;require.register("views/templates/dashboard", function(exports, require, module) {
var __templateData = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, functionType="function", escapeExpression=this.escapeExpression;


  buffer += "<header class=\"jumbotron subhead\" id=\"overview\">\n    <h1>Singularity</h1>\n</header>\n\n<section>\n    <div class=\"page-header\">\n        <h1>Status Overview</h1>\n    </div>\n    <div class=\"row-fluid\">\n        <div class=\"span4\">\n            <div class=\"well\">\n                <h3>Up for <span data-state-property=\"uptimeHuman\">"
    + escapeExpression(((stack1 = ((stack1 = depth0.state),stack1 == null || stack1 === false ? stack1 : stack1.uptimeHuman)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</span> &nbsp;&nbsp;&nbsp;</h3>\n            </div>\n        </div>\n        <div class=\"span4\">\n            <div class=\"well\">\n                <h3><span data-state-property=\"driverStatus\">"
    + escapeExpression(((stack1 = ((stack1 = depth0.state),stack1 == null || stack1 === false ? stack1 : stack1.driverStatus)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</span></h3>\n            </div>\n        </div>\n        <div class=\"span4\"></div>\n    </div>\n</section>\n\n<section>\n    <div class=\"page-header\">\n        <h1>Requests</h1>\n    </div>\n    <div class=\"row-fluid\">\n        <div class=\"span4\">\n            <div class=\"well\">\n                <div class=\"big-number\">\n                    <div class=\"number\" data-state-property=\"requests\">"
    + escapeExpression(((stack1 = ((stack1 = depth0.state),stack1 == null || stack1 === false ? stack1 : stack1.requests)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</div>\n                    <div class=\"number-label\">Active</div>\n                </div>\n            </div>\n        </div>\n        <div class=\"span4\">\n            <div class=\"well\">\n                <div class=\"big-number\">\n                    <div class=\"number\" data-state-property=\"pendingRequests\">"
    + escapeExpression(((stack1 = ((stack1 = depth0.state),stack1 == null || stack1 === false ? stack1 : stack1.pendingRequests)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</div>\n                    <div class=\"number-label\">Pending</div>\n                </div>\n            </div>\n        </div>\n        <div class=\"span4\">\n            <div class=\"well\">\n                <div class=\"big-number\">\n                    <div class=\"number\" data-state-property=\"cleaningRequests\">"
    + escapeExpression(((stack1 = ((stack1 = depth0.state),stack1 == null || stack1 === false ? stack1 : stack1.cleaningRequests)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</div>\n                    <div class=\"number-label\">Cleaning</div>\n                </div>\n            </div>\n        </div>\n    </div>\n</section>\n\n<section>\n    <div class=\"page-header\">\n        <h1>Tasks</h1>\n    </div>\n    <div class=\"row-fluid\">\n        <div class=\"span4\">\n            <div class=\"well\">\n                <div class=\"big-number\">\n                    <div class=\"number\" data-state-property=\"activeTasks\">"
    + escapeExpression(((stack1 = ((stack1 = depth0.state),stack1 == null || stack1 === false ? stack1 : stack1.activeTasks)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</div>\n                    <div class=\"number-label\">Active</div>\n                </div>\n            </div>\n        </div>\n        <div class=\"span4\">\n            <div class=\"well\">\n                <div class=\"big-number\">\n                    <div class=\"number\" data-state-property=\"scheduledTasks\">"
    + escapeExpression(((stack1 = ((stack1 = depth0.state),stack1 == null || stack1 === false ? stack1 : stack1.scheduledTasks)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</div>\n                    <div class=\"number-label\">Scheduled</div>\n                </div>\n            </div>\n        </div>\n        <div class=\"span4\"></div>\n    </div>\n</section>";
  return buffer;
  });
if (typeof define === 'function' && define.amd) {
  define([], function() {
    return __templateData;
  });
} else if (typeof module === 'object' && module && module.exports) {
  module.exports = __templateData;
} else {
  __templateData;
}
});

;require.register("views/templates/request", function(exports, require, module) {
var __templateData = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, stack2, options, functionType="function", escapeExpression=this.escapeExpression, self=this, helperMissing=helpers.helperMissing;

function program1(depth0,data) {
  
  var buffer = "";
  return buffer;
  }

function program3(depth0,data) {
  
  var buffer = "", stack1, stack2, options;
  buffer += "\n        <div class=\"row-fluid\">\n            <div class=\"span12\">\n                <table class=\"table\">\n                    <thead>\n                        <tr>\n                            <th>Name</th>\n                            <th>JSON</th>\n                        </tr>\n                    </thead>\n                    <tbody>\n                        ";
  options = {hash:{},inverse:self.noop,fn:self.program(4, program4, data),data:data};
  stack2 = ((stack1 = helpers.eachWithFn || depth0.eachWithFn),stack1 ? stack1.call(depth0, depth0.requestTasks, options) : helperMissing.call(depth0, "eachWithFn", depth0.requestTasks, options));
  if(stack2 || stack2 === 0) { buffer += stack2; }
  buffer += "\n                    </tbody>\n                </table>\n            </div>\n        </div>\n    ";
  return buffer;
  }
function program4(depth0,data) {
  
  var buffer = "", stack1, stack2, options;
  buffer += "\n                            <tr data-task-id=\"";
  if (stack1 = helpers.id) { stack1 = stack1.call(depth0, {hash:{},data:data}); }
  else { stack1 = depth0.id; stack1 = typeof stack1 === functionType ? stack1.apply(depth0) : stack1; }
  buffer += escapeExpression(stack1)
    + "\">\n                                <td><span class=\"simptip-position-top simptip-movable\" data-tooltip=\"";
  if (stack1 = helpers.id) { stack1 = stack1.call(depth0, {hash:{},data:data}); }
  else { stack1 = depth0.id; stack1 = typeof stack1 === functionType ? stack1.apply(depth0) : stack1; }
  buffer += escapeExpression(stack1)
    + "\"><a href=\"/singularity/task/";
  if (stack1 = helpers.id) { stack1 = stack1.call(depth0, {hash:{},data:data}); }
  else { stack1 = depth0.id; stack1 = typeof stack1 === functionType ? stack1.apply(depth0) : stack1; }
  buffer += escapeExpression(stack1)
    + "\" data-route=\"task/";
  if (stack1 = helpers.id) { stack1 = stack1.call(depth0, {hash:{},data:data}); }
  else { stack1 = depth0.id; stack1 = typeof stack1 === functionType ? stack1.apply(depth0) : stack1; }
  buffer += escapeExpression(stack1)
    + "\">";
  options = {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data};
  stack2 = ((stack1 = helpers.hardBreak || depth0.hardBreak),stack1 ? stack1.call(depth0, depth0.name, options) : helperMissing.call(depth0, "hardBreak", depth0.name, options));
  if(stack2 || stack2 === 0) { buffer += stack2; }
  buffer += "</a></span></td>\n                                <td><span><a data-task-id=\"";
  if (stack2 = helpers.id) { stack2 = stack2.call(depth0, {hash:{},data:data}); }
  else { stack2 = depth0.id; stack2 = typeof stack2 === functionType ? stack2.apply(depth0) : stack2; }
  buffer += escapeExpression(stack2)
    + "\" class=\"dont-route view-json\">JSON</a></span></td>\n                            </tr>\n                        ";
  return buffer;
  }

function program6(depth0,data) {
  
  
  return "\n        <div class=\"page-loader centered\"></div>\n    ";
  }

  buffer += "<header class=\"jumbotron subhead\" id=\"overview\">\n    <h1>";
  options = {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data};
  stack2 = ((stack1 = helpers.hardBreak || depth0.hardBreak),stack1 ? stack1.call(depth0, ((stack1 = depth0.request),stack1 == null || stack1 === false ? stack1 : stack1.name), options) : helperMissing.call(depth0, "hardBreak", ((stack1 = depth0.request),stack1 == null || stack1 === false ? stack1 : stack1.name), options));
  if(stack2 || stack2 === 0) { buffer += stack2; }
  buffer += "</h1>\n    <h2>";
  options = {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data};
  stack2 = ((stack1 = helpers.hardBreak || depth0.hardBreak),stack1 ? stack1.call(depth0, ((stack1 = depth0.request),stack1 == null || stack1 === false ? stack1 : stack1.id), options) : helperMissing.call(depth0, "hardBreak", ((stack1 = depth0.request),stack1 == null || stack1 === false ? stack1 : stack1.id), options));
  if(stack2 || stack2 === 0) { buffer += stack2; }
  buffer += "</h2>\n</header>\n\n<section>\n    <div class=\"page-header\">\n        <h1>Tasks</h1>\n    </div>\n    ";
  stack2 = helpers['if'].call(depth0, depth0.requestTasks, {hash:{},inverse:self.program(6, program6, data),fn:self.program(3, program3, data),data:data});
  if(stack2 || stack2 === 0) { buffer += stack2; }
  buffer += "\n    <div class=\"page-header\">\n        <h1>JSON</h1>\n    </div>\n    <div class=\"row-fluid\">\n        <div class=\"span12\">\n            <pre>"
    + escapeExpression(((stack1 = ((stack1 = depth0.request),stack1 == null || stack1 === false ? stack1 : stack1.JSONString)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</pre>\n        </div>\n    </div>\n</section>";
  return buffer;
  });
if (typeof define === 'function' && define.amd) {
  define([], function() {
    return __templateData;
  });
} else if (typeof module === 'object' && module && module.exports) {
  module.exports = __templateData;
} else {
  __templateData;
}
});

;require.register("views/templates/requests", function(exports, require, module) {
var __templateData = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, stack2, options, functionType="function", escapeExpression=this.escapeExpression, self=this, helperMissing=helpers.helperMissing;

function program1(depth0,data) {
  
  var buffer = "", stack1, stack2, options;
  buffer += "\n                        <tr data-request-id=\"";
  if (stack1 = helpers.id) { stack1 = stack1.call(depth0, {hash:{},data:data}); }
  else { stack1 = depth0.id; stack1 = typeof stack1 === functionType ? stack1.apply(depth0) : stack1; }
  buffer += escapeExpression(stack1)
    + "\">\n                            <td><span class=\"simptip-position-top simptip-movable\" data-tooltip=\"";
  if (stack1 = helpers.id) { stack1 = stack1.call(depth0, {hash:{},data:data}); }
  else { stack1 = depth0.id; stack1 = typeof stack1 === functionType ? stack1.apply(depth0) : stack1; }
  buffer += escapeExpression(stack1)
    + "\"><a href=\"/singularity/request/";
  if (stack1 = helpers.id) { stack1 = stack1.call(depth0, {hash:{},data:data}); }
  else { stack1 = depth0.id; stack1 = typeof stack1 === functionType ? stack1.apply(depth0) : stack1; }
  buffer += escapeExpression(stack1)
    + "\" data-route=\"request/";
  if (stack1 = helpers.id) { stack1 = stack1.call(depth0, {hash:{},data:data}); }
  else { stack1 = depth0.id; stack1 = typeof stack1 === functionType ? stack1.apply(depth0) : stack1; }
  buffer += escapeExpression(stack1)
    + "\">";
  options = {hash:{},inverse:self.noop,fn:self.program(2, program2, data),data:data};
  stack2 = ((stack1 = helpers.hardBreak || depth0.hardBreak),stack1 ? stack1.call(depth0, depth0.name, options) : helperMissing.call(depth0, "hardBreak", depth0.name, options));
  if(stack2 || stack2 === 0) { buffer += stack2; }
  buffer += "</a></span></td>\n                            <td><span class=\"simptip-position-top simptip-movable\" data-tooltip=\"";
  if (stack2 = helpers.timestamp) { stack2 = stack2.call(depth0, {hash:{},data:data}); }
  else { stack2 = depth0.timestamp; stack2 = typeof stack2 === functionType ? stack2.apply(depth0) : stack2; }
  buffer += escapeExpression(stack2)
    + "\">";
  if (stack2 = helpers.timestampHuman) { stack2 = stack2.call(depth0, {hash:{},data:data}); }
  else { stack2 = depth0.timestampHuman; stack2 = typeof stack2 === functionType ? stack2.apply(depth0) : stack2; }
  buffer += escapeExpression(stack2)
    + "</span></td>\n                            <td><span>";
  if (stack2 = helpers.deployUser) { stack2 = stack2.call(depth0, {hash:{},data:data}); }
  else { stack2 = depth0.deployUser; stack2 = typeof stack2 === functionType ? stack2.apply(depth0) : stack2; }
  buffer += escapeExpression(stack2)
    + "</span></td>\n                            <td><span>";
  if (stack2 = helpers.instances) { stack2 = stack2.call(depth0, {hash:{},data:data}); }
  else { stack2 = depth0.instances; stack2 = typeof stack2 === functionType ? stack2.apply(depth0) : stack2; }
  buffer += escapeExpression(stack2)
    + "</span></td>\n                            <td><span>";
  if (stack2 = helpers.daemon) { stack2 = stack2.call(depth0, {hash:{},data:data}); }
  else { stack2 = depth0.daemon; stack2 = typeof stack2 === functionType ? stack2.apply(depth0) : stack2; }
  buffer += escapeExpression(stack2)
    + "</span></td>\n                            <td><span><a data-request-id=\"";
  if (stack2 = helpers.id) { stack2 = stack2.call(depth0, {hash:{},data:data}); }
  else { stack2 = depth0.id; stack2 = typeof stack2 === functionType ? stack2.apply(depth0) : stack2; }
  buffer += escapeExpression(stack2)
    + "\" class=\"dont-route view-json\">JSON</a></span><td>\n                        </tr>\n                    ";
  return buffer;
  }
function program2(depth0,data) {
  
  var buffer = "";
  return buffer;
  }

  buffer += "<header class=\"jumbotron subhead\" id=\"overview\">\n    <h1>Requests</h1>\n</header>\n\n<section>\n    <div class=\"row-fluid\">\n        <input type=\"search\" placeholder=\"Search requests and tasks...\" required />\n    </div>\n    <div class=\"row-fluid\">\n        <div class=\"span12\">\n            <table class=\"table\">\n                <thead>\n                    <tr>\n                        <th>Name</th>\n                        <th>Requested</th>\n                        <th>Deploy User</th>\n                        <th>Instances</th>\n                        <th>Daemon</th>\n                        <th>JSON</th>\n                    </tr>\n                </thead>\n                <tbody>\n                    ";
  options = {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data};
  stack2 = ((stack1 = helpers.eachWithFn || depth0.eachWithFn),stack1 ? stack1.call(depth0, depth0.requests, options) : helperMissing.call(depth0, "eachWithFn", depth0.requests, options));
  if(stack2 || stack2 === 0) { buffer += stack2; }
  buffer += "\n                </tbody>\n            </table>\n        </div>\n    </div>\n</section>";
  return buffer;
  });
if (typeof define === 'function' && define.amd) {
  define([], function() {
    return __templateData;
  });
} else if (typeof module === 'object' && module && module.exports) {
  module.exports = __templateData;
} else {
  __templateData;
}
});

;require.register("views/templates/task", function(exports, require, module) {
var __templateData = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, stack2, options, self=this, helperMissing=helpers.helperMissing, functionType="function", escapeExpression=this.escapeExpression;

function program1(depth0,data) {
  
  var buffer = "";
  return buffer;
  }

  buffer += "<header class=\"jumbotron subhead\" id=\"overview\">\n    <h1>";
  options = {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data};
  stack2 = ((stack1 = helpers.hardBreak || depth0.hardBreak),stack1 ? stack1.call(depth0, ((stack1 = depth0.task),stack1 == null || stack1 === false ? stack1 : stack1.name), options) : helperMissing.call(depth0, "hardBreak", ((stack1 = depth0.task),stack1 == null || stack1 === false ? stack1 : stack1.name), options));
  if(stack2 || stack2 === 0) { buffer += stack2; }
  buffer += "</h1>\n    <h2>";
  options = {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data};
  stack2 = ((stack1 = helpers.hardBreak || depth0.hardBreak),stack1 ? stack1.call(depth0, ((stack1 = depth0.task),stack1 == null || stack1 === false ? stack1 : stack1.id), options) : helperMissing.call(depth0, "hardBreak", ((stack1 = depth0.task),stack1 == null || stack1 === false ? stack1 : stack1.id), options));
  if(stack2 || stack2 === 0) { buffer += stack2; }
  buffer += "</h2>\n</header>\n\n<section>\n    <div class=\"page-header\">\n        <h1>JSON</h1>\n    </div>\n    <div class=\"row-fluid\">\n        <div class=\"span12\">\n            <pre>"
    + escapeExpression(((stack1 = ((stack1 = depth0.task),stack1 == null || stack1 === false ? stack1 : stack1.JSONString)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</pre>\n        </div>\n    </div>\n</section>";
  return buffer;
  });
if (typeof define === 'function' && define.amd) {
  define([], function() {
    return __templateData;
  });
} else if (typeof module === 'object' && module && module.exports) {
  module.exports = __templateData;
} else {
  __templateData;
}
});

;require.register("views/templates/tasks", function(exports, require, module) {
var __templateData = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
  this.compilerInfo = [4,'>= 1.0.0'];
helpers = this.merge(helpers, Handlebars.helpers); data = data || {};
  var buffer = "", stack1, stack2, options, functionType="function", escapeExpression=this.escapeExpression, self=this, helperMissing=helpers.helperMissing;

function program1(depth0,data) {
  
  var buffer = "", stack1, stack2, options;
  buffer += "\n                        <tr data-task-id=\"";
  if (stack1 = helpers.id) { stack1 = stack1.call(depth0, {hash:{},data:data}); }
  else { stack1 = depth0.id; stack1 = typeof stack1 === functionType ? stack1.apply(depth0) : stack1; }
  buffer += escapeExpression(stack1)
    + "\">\n                            <td><span><a class=\"simptip-position-top simptip-movable\" data-tooltip=\"";
  if (stack1 = helpers.id) { stack1 = stack1.call(depth0, {hash:{},data:data}); }
  else { stack1 = depth0.id; stack1 = typeof stack1 === functionType ? stack1.apply(depth0) : stack1; }
  buffer += escapeExpression(stack1)
    + "\" href=\"singularity/task/";
  if (stack1 = helpers.id) { stack1 = stack1.call(depth0, {hash:{},data:data}); }
  else { stack1 = depth0.id; stack1 = typeof stack1 === functionType ? stack1.apply(depth0) : stack1; }
  buffer += escapeExpression(stack1)
    + "\" data-route=\"task/";
  if (stack1 = helpers.id) { stack1 = stack1.call(depth0, {hash:{},data:data}); }
  else { stack1 = depth0.id; stack1 = typeof stack1 === functionType ? stack1.apply(depth0) : stack1; }
  buffer += escapeExpression(stack1)
    + "\">";
  options = {hash:{},inverse:self.noop,fn:self.program(2, program2, data),data:data};
  stack2 = ((stack1 = helpers.hardBreak || depth0.hardBreak),stack1 ? stack1.call(depth0, depth0.name, options) : helperMissing.call(depth0, "hardBreak", depth0.name, options));
  if(stack2 || stack2 === 0) { buffer += stack2; }
  buffer += "</span></a></td>\n                            <td><span class=\"ellipsis simptip-position-top simptip-movable\" data-tooltip=\"";
  if (stack2 = helpers.startedAt) { stack2 = stack2.call(depth0, {hash:{},data:data}); }
  else { stack2 = depth0.startedAt; stack2 = typeof stack2 === functionType ? stack2.apply(depth0) : stack2; }
  buffer += escapeExpression(stack2)
    + "\">";
  if (stack2 = helpers.startedAtHuman) { stack2 = stack2.call(depth0, {hash:{},data:data}); }
  else { stack2 = depth0.startedAtHuman; stack2 = typeof stack2 === functionType ? stack2.apply(depth0) : stack2; }
  buffer += escapeExpression(stack2)
    + "</span></td>\n                            <td><span>";
  if (stack2 = helpers.host) { stack2 = stack2.call(depth0, {hash:{},data:data}); }
  else { stack2 = depth0.host; stack2 = typeof stack2 === functionType ? stack2.apply(depth0) : stack2; }
  buffer += escapeExpression(stack2)
    + "</span></td>\n                            <td><span>"
    + escapeExpression(((stack1 = ((stack1 = depth0.resources),stack1 == null || stack1 === false ? stack1 : stack1.cpus)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "</span></td>\n                            <td><span>"
    + escapeExpression(((stack1 = ((stack1 = depth0.resources),stack1 == null || stack1 === false ? stack1 : stack1.memoryMb)),typeof stack1 === functionType ? stack1.apply(depth0) : stack1))
    + "Mb</span></td>\n                            <td><span><a data-task-id=\"";
  if (stack2 = helpers.id) { stack2 = stack2.call(depth0, {hash:{},data:data}); }
  else { stack2 = depth0.id; stack2 = typeof stack2 === functionType ? stack2.apply(depth0) : stack2; }
  buffer += escapeExpression(stack2)
    + "\" class=\"dont-route view-json\">JSON</a></span><td>\n                        </tr>\n                    ";
  return buffer;
  }
function program2(depth0,data) {
  
  var buffer = "";
  return buffer;
  }

function program4(depth0,data) {
  
  var buffer = "", stack1, stack2, options;
  buffer += "\n                        <tr data-task-id=\"";
  if (stack1 = helpers.id) { stack1 = stack1.call(depth0, {hash:{},data:data}); }
  else { stack1 = depth0.id; stack1 = typeof stack1 === functionType ? stack1.apply(depth0) : stack1; }
  buffer += escapeExpression(stack1)
    + "\">\n                            <td><span class=\"simptip-position-top simptip-movable\" data-tooltip=\"";
  if (stack1 = helpers.id) { stack1 = stack1.call(depth0, {hash:{},data:data}); }
  else { stack1 = depth0.id; stack1 = typeof stack1 === functionType ? stack1.apply(depth0) : stack1; }
  buffer += escapeExpression(stack1)
    + "\">";
  options = {hash:{},inverse:self.noop,fn:self.program(2, program2, data),data:data};
  stack2 = ((stack1 = helpers.hardBreak || depth0.hardBreak),stack1 ? stack1.call(depth0, depth0.name, options) : helperMissing.call(depth0, "hardBreak", depth0.name, options));
  if(stack2 || stack2 === 0) { buffer += stack2; }
  buffer += "</span></td>\n                            <td><span class=\"ellipsis simptip-position-top simptip-movable\" data-tooltip=\"";
  if (stack2 = helpers.nextRunAt) { stack2 = stack2.call(depth0, {hash:{},data:data}); }
  else { stack2 = depth0.nextRunAt; stack2 = typeof stack2 === functionType ? stack2.apply(depth0) : stack2; }
  buffer += escapeExpression(stack2)
    + "\">";
  if (stack2 = helpers.nextRunAtHuman) { stack2 = stack2.call(depth0, {hash:{},data:data}); }
  else { stack2 = depth0.nextRunAtHuman; stack2 = typeof stack2 === functionType ? stack2.apply(depth0) : stack2; }
  buffer += escapeExpression(stack2)
    + "</span></td>\n                            <td><span><a data-task-id=\"";
  if (stack2 = helpers.id) { stack2 = stack2.call(depth0, {hash:{},data:data}); }
  else { stack2 = depth0.id; stack2 = typeof stack2 === functionType ? stack2.apply(depth0) : stack2; }
  buffer += escapeExpression(stack2)
    + "\" class=\"dont-route view-json\">JSON</a></span><td>\n                        </tr>\n                    ";
  return buffer;
  }

  buffer += "<header class=\"jumbotron subhead\" id=\"overview\">\n    <h1>Tasks</h1>\n</header>\n\n<section>\n    <input type=\"search\" placeholder=\"Search tasks...\" required />\n    <div class=\"row-fluid\">\n        <div class=\"span12\">\n            <h2>Active Tasks</h2>\n            <table class=\"table\">\n                <thead>\n                    <tr>\n                        <th>Name</th>\n                        <th>Started</th>\n                        <th>Host</th>\n                        <th>CPUs</th>\n                        <th>Memory</th>\n                        <th>JSON</th>\n                    </tr>\n                </thead>\n                <tbody>\n                    ";
  options = {hash:{},inverse:self.noop,fn:self.program(1, program1, data),data:data};
  stack2 = ((stack1 = helpers.eachWithFn || depth0.eachWithFn),stack1 ? stack1.call(depth0, depth0.tasksActive, options) : helperMissing.call(depth0, "eachWithFn", depth0.tasksActive, options));
  if(stack2 || stack2 === 0) { buffer += stack2; }
  buffer += "\n                </tbody>\n            </table>\n        </div>\n    </div>\n    <div class=\"row-fluid\">\n        <div class=\"span12\">\n            <h2>Scheduled Tasks</h2>\n            <table class=\"table\">\n                <thead>\n                    <tr>\n                        <th>Name</th>\n                        <th>Scheduled</th>\n                        <th>JSON</th>\n                    </tr>\n                </thead>\n                <tbody>\n                    ";
  options = {hash:{},inverse:self.noop,fn:self.program(4, program4, data),data:data};
  stack2 = ((stack1 = helpers.eachWithFn || depth0.eachWithFn),stack1 ? stack1.call(depth0, depth0.tasksScheduled, options) : helperMissing.call(depth0, "eachWithFn", depth0.tasksScheduled, options));
  if(stack2 || stack2 === 0) { buffer += stack2; }
  buffer += "\n                </tbody>\n            </table>\n        </div>\n    </div>\n</section>";
  return buffer;
  });
if (typeof define === 'function' && define.amd) {
  define([], function() {
    return __templateData;
  });
} else if (typeof module === 'object' && module && module.exports) {
  module.exports = __templateData;
} else {
  __templateData;
}
});

;require.register("views/view", function(exports, require, module) {
var View, _ref,
  __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
  __hasProp = {}.hasOwnProperty,
  __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

require('lib/view_helper');

View = (function(_super) {
  __extends(View, _super);

  function View() {
    this.routeLink = __bind(this.routeLink, this);
    _ref = View.__super__.constructor.apply(this, arguments);
    return _ref;
  }

  View.prototype.el = '#page';

  View.prototype.events = {
    'click a': 'routeLink'
  };

  View.prototype.routeLink = function(e) {
    var $link, url;
    $link = $(e.target);
    url = $link.attr('href');
    if ($link.attr('target') === '_blank' || typeof url === 'undefined' || url.substr(0, 4) === 'http') {
      return true;
    }
    e.preventDefault();
    if (url.indexOf('.') === 0) {
      url = url.substring(1);
      if (url.indexOf('/') === 0) {
        url = url.substring(1);
      }
    }
    if ($link.data('route') || $link.data('route') === '') {
      url = $link.data('route');
    }
    return app.router.navigate(url, {
      trigger: true
    });
  };

  return View;

})(Backbone.View);

module.exports = View;
});

;
//# sourceMappingURL=app.js.map