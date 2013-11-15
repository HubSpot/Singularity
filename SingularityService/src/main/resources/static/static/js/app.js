(function(/*! Brunch !*/) {
  'use strict';

  var globals = typeof window !== 'undefined' ? window : global;
  if (typeof globals.require === 'function') return;

  var modules = {};
  var cache = {};

  var has = function(object, name) {
    return hasOwnProperty.call(object, name);
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
      return require(absolute);
    };
  };

  var initModule = function(name, definition) {
    var module = {id: name, exports: {}};
    definition(module.exports, localRequire(name), module);
    var exports = cache[name] = module.exports;
    return exports;
  };

  var require = function(name) {
    var path = expand(name, '.');

    if (has(cache, path)) return cache[path];
    if (has(modules, path)) return initModule(path, modules[path]);

    var dirIndex = expand(path, './index');
    if (has(cache, dirIndex)) return cache[dirIndex];
    if (has(modules, dirIndex)) return initModule(dirIndex, modules[dirIndex]);

    throw new Error('Cannot find module "' + name + '"');
  };

  var define = function(bundle) {
    for (var key in bundle) {
      if (has(bundle, key)) {
        modules[key] = bundle[key];
      }
    }
  }

  globals.require = require;
  globals.require.define = define;
  globals.require.brunch = true;
})();

window.require.define({"application": function(exports, require, module) {
  (function() {
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
          if (_this.resolve_countdown === 0) return success();
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

  }).call(this);
  
}});

window.require.define({"collections/Requests": function(exports, require, module) {
  (function() {
    var Collection, Requests,
      __hasProp = Object.prototype.hasOwnProperty,
      __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

    Collection = require('./collection');

    Requests = (function(_super) {

      __extends(Requests, _super);

      function Requests() {
        Requests.__super__.constructor.apply(this, arguments);
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
        var _ref, _ref2, _ref3;
        return ((_ref = (_ref2 = request.executorData) != null ? (_ref3 = _ref2.env) != null ? _ref3.DEPLOY_USER : void 0 : void 0) != null ? _ref : '').split('@')[0];
      };

      Requests.prototype.comparator = 'name';

      return Requests;

    })(Collection);

    module.exports = Requests;

  }).call(this);
  
}});

window.require.define({"collections/Tasks": function(exports, require, module) {
  (function() {
    var Collection, Tasks,
      __hasProp = Object.prototype.hasOwnProperty,
      __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

    Collection = require('./collection');

    Tasks = (function(_super) {

      __extends(Tasks, _super);

      function Tasks() {
        Tasks.__super__.constructor.apply(this, arguments);
      }

      Tasks.prototype.comparator = 'name';

      return Tasks;

    })(Collection);

    module.exports = Tasks;

  }).call(this);
  
}});

window.require.define({"collections/TasksActive": function(exports, require, module) {
  (function() {
    var Collection, Tasks, TasksActive,
      __hasProp = Object.prototype.hasOwnProperty,
      __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

    Collection = require('./collection');

    Tasks = require('./Tasks');

    TasksActive = (function(_super) {

      __extends(TasksActive, _super);

      function TasksActive() {
        TasksActive.__super__.constructor.apply(this, arguments);
      }

      TasksActive.prototype.url = "http://" + env.SINGULARITY_BASE + "/" + constants.api_base + "/tasks/active";

      TasksActive.prototype.parse = function(tasks) {
        var _this = this;
        _.each(tasks, function(task, i) {
          var _ref;
          task.id = task.task.taskId.value;
          task.name = task.task.name;
          task.resources = task.taskRequest.request.resources;
          task.host = (_ref = task.offer.hostname) != null ? _ref.split('.')[0] : void 0;
          task.startedAt = task.taskId.startedAt;
          task.startedAtHuman = moment(task.taskId.startedAt).from();
          task.JSONString = utils.stringJSON(task);
          return tasks[i] = task;
        });
        return tasks;
      };

      TasksActive.prototype.comparator = 'name';

      return TasksActive;

    })(Tasks);

    module.exports = TasksActive;

  }).call(this);
  
}});

window.require.define({"collections/TasksScheduled": function(exports, require, module) {
  (function() {
    var Collection, Tasks, TasksScheduled,
      __hasProp = Object.prototype.hasOwnProperty,
      __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

    Collection = require('./collection');

    Tasks = require('./Tasks');

    TasksScheduled = (function(_super) {

      __extends(TasksScheduled, _super);

      function TasksScheduled() {
        TasksScheduled.__super__.constructor.apply(this, arguments);
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

  }).call(this);
  
}});

window.require.define({"collections/collection": function(exports, require, module) {
  (function() {
    var Collection,
      __hasProp = Object.prototype.hasOwnProperty,
      __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

    module.exports = Collection = (function(_super) {

      __extends(Collection, _super);

      function Collection() {
        Collection.__super__.constructor.apply(this, arguments);
      }

      return Collection;

    })(Backbone.Collection);

  }).call(this);
  
}});

window.require.define({"constants": function(exports, require, module) {
  (function() {
    var constants;

    constants = {
      app_name: 'singularity',
      config_server_base: '/singularity',
      api_base: 'singularity2/v1',
      kumonga_api_base: 'kumonga/v2'
    };

    module.exports = constants;

  }).call(this);
  
}});

window.require.define({"env": function(exports, require, module) {
  (function() {

    module.exports = {
      env: 'local',
      SINGULARITY_BASE: 'heliograph.iad01.hubspot-networks.net:7005'
    };

  }).call(this);
  
}});

window.require.define({"initialize": function(exports, require, module) {
  (function() {

    window.env = require('env');

    window.utils = require('utils');

    window.constants = require('constants');

    window.app = require('application');

    _.mixin(_.string.exports());

    vex.defaultOptions.className = 'vex-theme-default';

    $(function() {
      return app.initialize();
    });

  }).call(this);
  
}});

window.require.define({"lib/login": function(exports, require, module) {
  (function() {



  }).call(this);
  
}});

window.require.define({"lib/router": function(exports, require, module) {
  (function() {
    var DashboardView, NavigationView, PageNotFoundView, RequestView, RequestsView, Router, TaskView, TasksView, nav,
      __hasProp = Object.prototype.hasOwnProperty,
      __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

    DashboardView = require('views/dashboard');

    RequestsView = require('views/requests');

    RequestView = require('views/request');

    TasksView = require('views/tasks');

    TaskView = require('views/task');

    PageNotFoundView = require('views/page_not_found');

    NavigationView = require('views/navigation');

    nav = function() {
      if (!(app.views.navigationView != null)) {
        app.views.navigationView = new NavigationView;
      }
      return app.views.navigationView.render();
    };

    Router = (function(_super) {

      __extends(Router, _super);

      function Router() {
        Router.__super__.constructor.apply(this, arguments);
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
        if (!(app.views.dashboard != null)) app.views.dashboard = new DashboardView;
        app.views.current = app.views.dashboard;
        return app.views.dashboard.render();
      };

      Router.prototype.requests = function() {
        nav();
        if (!(app.views.requests != null)) app.views.requests = new RequestsView;
        app.views.current = app.views.requests;
        return app.views.requests.render();
      };

      Router.prototype.request = function(requestId) {
        nav();
        if (!app.views.requestViews) app.views.requestViews = {};
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
        if (!(app.views.tasks != null)) app.views.tasks = new TasksView;
        app.views.current = app.views.tasks;
        return app.views.tasks.render();
      };

      Router.prototype.task = function(taskId) {
        nav();
        if (!app.views.taskViews) app.views.taskViews = {};
        if (!app.views.taskViews[taskId]) {
          app.views.taskViews[taskId] = new TaskView({
            taskId: taskId
          });
        }
        app.views.current = app.views.taskViews[taskId];
        return app.views.taskViews[taskId].render();
      };

      Router.prototype.templateFromURLFragment = function() {
        var template;
        nav();
        app.views.current = void 0;
        template = void 0;
        try {
          template = require("../views/templates/" + Backbone.history.fragment);
        } catch (error) {

        }
        if (template) {
          $('body > .app').html(template);
          return;
        }
        return this.show404();
      };

      Router.prototype.show404 = function() {
        nav();
        if (!(app.views.pageNotFound != null)) {
          app.views.pageNotFound = new PageNotFoundView;
        }
        app.views.current = app.views.pageNotFound;
        return app.views.pageNotFound.render();
      };

      return Router;

    })(Backbone.Router);

    module.exports = Router;

  }).call(this);
  
}});

window.require.define({"lib/view_helper": function(exports, require, module) {
  (function() {

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

  }).call(this);
  
}});

window.require.define({"models/RequestTasks": function(exports, require, module) {
  (function() {
    var Model, RequestTasks,
      __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
      __hasProp = Object.prototype.hasOwnProperty,
      __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

    Model = require('./model');

    RequestTasks = (function(_super) {

      __extends(RequestTasks, _super);

      function RequestTasks() {
        this.initialize = __bind(this.initialize, this);
        this.url = __bind(this.url, this);
        RequestTasks.__super__.constructor.apply(this, arguments);
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

  }).call(this);
  
}});

window.require.define({"models/State": function(exports, require, module) {
  (function() {
    var Model, State,
      __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
      __hasProp = Object.prototype.hasOwnProperty,
      __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

    Model = require('./model');

    State = (function(_super) {

      __extends(State, _super);

      function State() {
        this.parse = __bind(this.parse, this);
        State.__super__.constructor.apply(this, arguments);
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

  }).call(this);
  
}});

window.require.define({"models/Task": function(exports, require, module) {
  (function() {
    var Model, Task,
      __hasProp = Object.prototype.hasOwnProperty,
      __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

    Model = require('./model');

    Task = (function(_super) {

      __extends(Task, _super);

      function Task() {
        Task.__super__.constructor.apply(this, arguments);
      }

      Task.prototype.url = function() {
        return "http://" + env.SINGULARITY_BASE + "/" + constants.api_base + "/task/" + (this.get('name'));
      };

      return Task;

    })(Model);

    module.exports = Task;

  }).call(this);
  
}});

window.require.define({"models/model": function(exports, require, module) {
  (function() {
    var Model,
      __hasProp = Object.prototype.hasOwnProperty,
      __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

    module.exports = Model = (function(_super) {

      __extends(Model, _super);

      function Model() {
        Model.__super__.constructor.apply(this, arguments);
      }

      return Model;

    })(Backbone.Model);

  }).call(this);
  
}});

window.require.define({"utils": function(exports, require, module) {
  (function() {
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

  }).call(this);
  
}});

window.require.define({"views/dashboard": function(exports, require, module) {
  (function() {
    var DashboardView, View,
      __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
      __hasProp = Object.prototype.hasOwnProperty,
      __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

    View = require('./view');

    DashboardView = (function(_super) {

      __extends(DashboardView, _super);

      function DashboardView() {
        this.render = __bind(this.render, this);
        DashboardView.__super__.constructor.apply(this, arguments);
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

  }).call(this);
  
}});

window.require.define({"views/navigation": function(exports, require, module) {
  (function() {
    var NavigationView, View,
      __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
      __hasProp = Object.prototype.hasOwnProperty,
      __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

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
        NavigationView.__super__.constructor.apply(this, arguments);
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
        if (subtitle !== '') subtitle = ' — ' + subtitle;
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
        if ($collapse.data().collapse) return $collapse.collapse('hide');
      };

      return NavigationView;

    })(View);

    module.exports = NavigationView;

  }).call(this);
  
}});

window.require.define({"views/page_not_found": function(exports, require, module) {
  (function() {
    var PageNotFoundView, View,
      __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
      __hasProp = Object.prototype.hasOwnProperty,
      __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

    View = require('./view');

    PageNotFoundView = (function(_super) {

      __extends(PageNotFoundView, _super);

      function PageNotFoundView() {
        this.render = __bind(this.render, this);
        PageNotFoundView.__super__.constructor.apply(this, arguments);
      }

      PageNotFoundView.prototype.template = require('./templates/404');

      PageNotFoundView.prototype.render = function() {
        return $(this.el).html(this.template);
      };

      return PageNotFoundView;

    })(View);

    module.exports = PageNotFoundView;

  }).call(this);
  
}});

window.require.define({"views/request": function(exports, require, module) {
  (function() {
    var RequestTasks, RequestView, View,
      __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
      __hasProp = Object.prototype.hasOwnProperty,
      __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

    View = require('./view');

    RequestTasks = require('../models/RequestTasks');

    RequestView = (function(_super) {

      __extends(RequestView, _super);

      function RequestView() {
        this.render = __bind(this.render, this);
        this.initialize = __bind(this.initialize, this);
        RequestView.__super__.constructor.apply(this, arguments);
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
        if (!this.request) return false;
        context = {
          request: this.request.toJSON(),
          requestTasks: this.requestTasks.toJSON()
        };
        this.$el.html(this.template(context));
        return this.setupEvents();
      };

      RequestView.prototype.setupEvents = function() {
        return this.$el.find('.view-json').unbind('click').click(function(event) {
          var _ref;
          return utils.viewJSON((_ref = utils.getAcrossCollections([app.collections.tasksActive, app.collections.tasksScheduled], $(event.target).data('task-id'))) != null ? _ref.toJSON() : void 0);
        });
      };

      return RequestView;

    })(View);

    module.exports = RequestView;

  }).call(this);
  
}});

window.require.define({"views/requests": function(exports, require, module) {
  (function() {
    var RequestsView, View,
      __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
      __hasProp = Object.prototype.hasOwnProperty,
      __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

    View = require('./view');

    RequestsView = (function(_super) {

      __extends(RequestsView, _super);

      function RequestsView() {
        this.setUpSearchEvents = __bind(this.setUpSearchEvents, this);
        this.render = __bind(this.render, this);
        RequestsView.__super__.constructor.apply(this, arguments);
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
          if (text === '') $rows.removeClass('filtered');
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

  }).call(this);
  
}});

window.require.define({"views/task": function(exports, require, module) {
  (function() {
    var TaskView, View,
      __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
      __hasProp = Object.prototype.hasOwnProperty,
      __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

    View = require('./view');

    TaskView = (function(_super) {

      __extends(TaskView, _super);

      function TaskView() {
        this.render = __bind(this.render, this);
        this.initialize = __bind(this.initialize, this);
        TaskView.__super__.constructor.apply(this, arguments);
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

  }).call(this);
  
}});

window.require.define({"views/tasks": function(exports, require, module) {
  (function() {
    var TasksView, View,
      __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
      __hasProp = Object.prototype.hasOwnProperty,
      __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

    View = require('./view');

    TasksView = (function(_super) {

      __extends(TasksView, _super);

      function TasksView() {
        this.render = __bind(this.render, this);
        TasksView.__super__.constructor.apply(this, arguments);
      }

      TasksView.prototype.template = require('./templates/tasks');

      TasksView.prototype.render = function() {
        var context;
        context = {
          tasksActive: app.collections.tasksActive.toJSON(),
          tasksScheduled: app.collections.tasksScheduled.toJSON()
        };
        this.$el.html(this.template(context));
        return this.setupEvents();
      };

      TasksView.prototype.setupEvents = function() {
        return this.$el.find('.view-json').unbind('click').click(function(event) {
          var _ref;
          return utils.viewJSON((_ref = utils.getAcrossCollections([app.collections.tasksActive, app.collections.tasksScheduled], $(event.target).data('task-id'))) != null ? _ref.toJSON() : void 0);
        });
      };

      return TasksView;

    })(View);

    module.exports = TasksView;

  }).call(this);
  
}});

window.require.define({"views/templates/404": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var foundHelper, self=this;


    return "<h1>Page not found</h1>";});
}});

window.require.define({"views/templates/dashboard": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", stack1, foundHelper, self=this, functionType="function", helperMissing=helpers.helperMissing, undef=void 0, escapeExpression=this.escapeExpression;


    buffer += "<header class=\"jumbotron subhead\" id=\"overview\">\n    <h1>Singularity</h1>\n</header>\n\n<section>\n    <div class=\"page-header\">\n        <h1>Status Overview</h1>\n    </div>\n    <div class=\"row-fluid\">\n        <div class=\"span4\">\n            <div class=\"well\">\n                <h3>Up for <span data-state-property=\"uptimeHuman\">";
    foundHelper = helpers.state;
    stack1 = foundHelper || depth0.state;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.uptimeHuman);
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "state.uptimeHuman", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</span> &nbsp;&nbsp;&nbsp;</h3>\n            </div>\n        </div>\n        <div class=\"span4\">\n            <div class=\"well\">\n                <h3><span data-state-property=\"driverStatus\">";
    foundHelper = helpers.state;
    stack1 = foundHelper || depth0.state;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.driverStatus);
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "state.driverStatus", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</span></h3>\n            </div>\n        </div>\n        <div class=\"span4\"></div>\n    </div>\n</section>\n\n<section>\n    <div class=\"page-header\">\n        <h1>Requests</h1>\n    </div>\n    <div class=\"row-fluid\">\n        <div class=\"span4\">\n            <div class=\"well\">\n                <div class=\"big-number\">\n                    <div class=\"number\" data-state-property=\"requests\">";
    foundHelper = helpers.state;
    stack1 = foundHelper || depth0.state;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.requests);
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "state.requests", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</div>\n                    <div class=\"number-label\">Active</div>\n                </div>\n            </div>\n        </div>\n        <div class=\"span4\">\n            <div class=\"well\">\n                <div class=\"big-number\">\n                    <div class=\"number\" data-state-property=\"pendingRequests\">";
    foundHelper = helpers.state;
    stack1 = foundHelper || depth0.state;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.pendingRequests);
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "state.pendingRequests", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</div>\n                    <div class=\"number-label\">Pending</div>\n                </div>\n            </div>\n        </div>\n        <div class=\"span4\">\n            <div class=\"well\">\n                <div class=\"big-number\">\n                    <div class=\"number\" data-state-property=\"cleaningRequests\">";
    foundHelper = helpers.state;
    stack1 = foundHelper || depth0.state;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.cleaningRequests);
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "state.cleaningRequests", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</div>\n                    <div class=\"number-label\">Cleaning</div>\n                </div>\n            </div>\n        </div>\n    </div>\n</section>\n\n<section>\n    <div class=\"page-header\">\n        <h1>Tasks</h1>\n    </div>\n    <div class=\"row-fluid\">\n        <div class=\"span4\">\n            <div class=\"well\">\n                <div class=\"big-number\">\n                    <div class=\"number\" data-state-property=\"activeTasks\">";
    foundHelper = helpers.state;
    stack1 = foundHelper || depth0.state;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.activeTasks);
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "state.activeTasks", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</div>\n                    <div class=\"number-label\">Active</div>\n                </div>\n            </div>\n        </div>\n        <div class=\"span4\">\n            <div class=\"well\">\n                <div class=\"big-number\">\n                    <div class=\"number\" data-state-property=\"scheduledTasks\">";
    foundHelper = helpers.state;
    stack1 = foundHelper || depth0.state;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.scheduledTasks);
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "state.scheduledTasks", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</div>\n                    <div class=\"number-label\">Scheduled</div>\n                </div>\n            </div>\n        </div>\n        <div class=\"span4\"></div>\n    </div>\n</section>";
    return buffer;});
}});

window.require.define({"views/templates/request": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", stack1, stack2, foundHelper, tmp1, self=this, functionType="function", helperMissing=helpers.helperMissing, undef=void 0, escapeExpression=this.escapeExpression, blockHelperMissing=helpers.blockHelperMissing;

  function program1(depth0,data) {
    
    var buffer = "";
    return buffer;}

  function program3(depth0,data) {
    
    var buffer = "";
    return buffer;}

  function program5(depth0,data) {
    
    var buffer = "", stack1, stack2;
    buffer += "\n        <div class=\"row-fluid\">\n            <div class=\"span12\">\n                <table class=\"table\">\n                    <thead>\n                        <tr>\n                            <th>Name</th>\n                            <th>JSON</th>\n                        </tr>\n                    </thead>\n                    <tbody>\n                        ";
    foundHelper = helpers.requestTasks;
    stack1 = foundHelper || depth0.requestTasks;
    foundHelper = helpers.eachWithFn;
    stack2 = foundHelper || depth0.eachWithFn;
    tmp1 = self.program(6, program6, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack2, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n                    </tbody>\n                </table>\n            </div>\n        </div>\n    ";
    return buffer;}
  function program6(depth0,data) {
    
    var buffer = "", stack1, stack2;
    buffer += "\n                            <tr data-task-id=\"";
    foundHelper = helpers.id;
    stack1 = foundHelper || depth0.id;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "id", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\">\n                                <td><span class=\"simptip-position-top simptip-movable\" data-tooltip=\"";
    foundHelper = helpers.id;
    stack1 = foundHelper || depth0.id;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "id", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\"><a href=\"/singularity/task/";
    foundHelper = helpers.id;
    stack1 = foundHelper || depth0.id;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "id", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\" data-route=\"task/";
    foundHelper = helpers.id;
    stack1 = foundHelper || depth0.id;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "id", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\">";
    foundHelper = helpers.name;
    stack1 = foundHelper || depth0.name;
    foundHelper = helpers.hardBreak;
    stack2 = foundHelper || depth0.hardBreak;
    tmp1 = self.program(7, program7, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack2, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "</a></span></td>\n                                <td><span><a data-task-id=\"";
    foundHelper = helpers.id;
    stack1 = foundHelper || depth0.id;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "id", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\" class=\"dont-route view-json\">JSON</a></span></td>\n                            </tr>\n                        ";
    return buffer;}
  function program7(depth0,data) {
    
    var buffer = "";
    return buffer;}

  function program9(depth0,data) {
    
    
    return "\n        <div class=\"page-loader centered\"></div>\n    ";}

    buffer += "<header class=\"jumbotron subhead\" id=\"overview\">\n    <h1>";
    foundHelper = helpers.request;
    stack1 = foundHelper || depth0.request;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.name);
    foundHelper = helpers.hardBreak;
    stack2 = foundHelper || depth0.hardBreak;
    tmp1 = self.program(1, program1, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack2, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "</h1>\n    <h2>";
    foundHelper = helpers.request;
    stack1 = foundHelper || depth0.request;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.id);
    foundHelper = helpers.hardBreak;
    stack2 = foundHelper || depth0.hardBreak;
    tmp1 = self.program(3, program3, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack2, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "</h2>\n</header>\n\n<section>\n    <div class=\"page-header\">\n        <h1>Tasks</h1>\n    </div>\n    ";
    foundHelper = helpers.requestTasks;
    stack1 = foundHelper || depth0.requestTasks;
    stack2 = helpers['if'];
    tmp1 = self.program(5, program5, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.program(9, program9, data);
    stack1 = stack2.call(depth0, stack1, tmp1);
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n    <div class=\"page-header\">\n        <h1>JSON</h1>\n    </div>\n    <div class=\"row-fluid\">\n        <div class=\"span12\">\n            <pre>";
    foundHelper = helpers.request;
    stack1 = foundHelper || depth0.request;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.JSONString);
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "request.JSONString", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</pre>\n        </div>\n    </div>\n</section>";
    return buffer;});
}});

window.require.define({"views/templates/requests": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", stack1, stack2, foundHelper, tmp1, self=this, functionType="function", helperMissing=helpers.helperMissing, undef=void 0, escapeExpression=this.escapeExpression, blockHelperMissing=helpers.blockHelperMissing;

  function program1(depth0,data) {
    
    var buffer = "", stack1, stack2;
    buffer += "\n                        <tr data-request-id=\"";
    foundHelper = helpers.id;
    stack1 = foundHelper || depth0.id;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "id", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\">\n                            <td><span class=\"simptip-position-top simptip-movable\" data-tooltip=\"";
    foundHelper = helpers.id;
    stack1 = foundHelper || depth0.id;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "id", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\"><a href=\"/singularity/request/";
    foundHelper = helpers.id;
    stack1 = foundHelper || depth0.id;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "id", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\" data-route=\"request/";
    foundHelper = helpers.id;
    stack1 = foundHelper || depth0.id;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "id", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\">";
    foundHelper = helpers.name;
    stack1 = foundHelper || depth0.name;
    foundHelper = helpers.hardBreak;
    stack2 = foundHelper || depth0.hardBreak;
    tmp1 = self.program(2, program2, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack2, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "</a></span></td>\n                            <td><span class=\"simptip-position-top simptip-movable\" data-tooltip=\"";
    foundHelper = helpers.timestamp;
    stack1 = foundHelper || depth0.timestamp;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "timestamp", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\">";
    foundHelper = helpers.timestampHuman;
    stack1 = foundHelper || depth0.timestampHuman;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "timestampHuman", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</span></td>\n                            <td><span>";
    foundHelper = helpers.deployUser;
    stack1 = foundHelper || depth0.deployUser;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "deployUser", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</span></td>\n                            <td><span>";
    foundHelper = helpers.instances;
    stack1 = foundHelper || depth0.instances;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "instances", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</span></td>\n                            <td><span>";
    foundHelper = helpers.daemon;
    stack1 = foundHelper || depth0.daemon;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "daemon", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</span></td>\n                            <td><span><a data-request-id=\"";
    foundHelper = helpers.id;
    stack1 = foundHelper || depth0.id;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "id", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\" class=\"dont-route view-json\">JSON</a></span><td>\n                        </tr>\n                    ";
    return buffer;}
  function program2(depth0,data) {
    
    var buffer = "";
    return buffer;}

    buffer += "<header class=\"jumbotron subhead\" id=\"overview\">\n    <h1>Requests</h1>\n</header>\n\n<section>\n    <div class=\"page-header\">\n        <h1>Status Overview</h1>\n    </div>\n    <div class=\"row-fluid\">\n        <input type=\"search\" placeholder=\"Search requests and tasks...\" required />\n    </div>\n    <div class=\"row-fluid\">\n        <div class=\"span12\">\n            <table class=\"table\">\n                <thead>\n                    <tr>\n                        <th>Name</th>\n                        <th>Requested</th>\n                        <th>Deploy User</th>\n                        <th>Instances</th>\n                        <th>Daemon</th>\n                        <th>JSON</th>\n                    </tr>\n                </thead>\n                <tbody>\n                    ";
    foundHelper = helpers.requests;
    stack1 = foundHelper || depth0.requests;
    foundHelper = helpers.eachWithFn;
    stack2 = foundHelper || depth0.eachWithFn;
    tmp1 = self.program(1, program1, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack2, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n                </tbody>\n            </table>\n        </div>\n    </div>\n</section>";
    return buffer;});
}});

window.require.define({"views/templates/task": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", stack1, stack2, foundHelper, tmp1, self=this, functionType="function", blockHelperMissing=helpers.blockHelperMissing, helperMissing=helpers.helperMissing, undef=void 0, escapeExpression=this.escapeExpression;

  function program1(depth0,data) {
    
    var buffer = "";
    return buffer;}

  function program3(depth0,data) {
    
    var buffer = "";
    return buffer;}

    buffer += "<header class=\"jumbotron subhead\" id=\"overview\">\n    <h1>";
    foundHelper = helpers.task;
    stack1 = foundHelper || depth0.task;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.name);
    foundHelper = helpers.hardBreak;
    stack2 = foundHelper || depth0.hardBreak;
    tmp1 = self.program(1, program1, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack2, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "</h1>\n    <h2>";
    foundHelper = helpers.task;
    stack1 = foundHelper || depth0.task;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.id);
    foundHelper = helpers.hardBreak;
    stack2 = foundHelper || depth0.hardBreak;
    tmp1 = self.program(3, program3, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack2, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "</h2>\n</header>\n\n<section>\n    <div class=\"page-header\">\n        <h1>JSON</h1>\n    </div>\n    <div class=\"row-fluid\">\n        <div class=\"span12\">\n            <pre>";
    foundHelper = helpers.task;
    stack1 = foundHelper || depth0.task;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.JSONString);
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "task.JSONString", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</pre>\n        </div>\n    </div>\n</section>";
    return buffer;});
}});

window.require.define({"views/templates/tasks": function(exports, require, module) {
  module.exports = Handlebars.template(function (Handlebars,depth0,helpers,partials,data) {
    helpers = helpers || Handlebars.helpers;
    var buffer = "", stack1, stack2, foundHelper, tmp1, self=this, functionType="function", helperMissing=helpers.helperMissing, undef=void 0, escapeExpression=this.escapeExpression, blockHelperMissing=helpers.blockHelperMissing;

  function program1(depth0,data) {
    
    var buffer = "", stack1, stack2;
    buffer += "\n                        <tr data-task-id=\"";
    foundHelper = helpers.id;
    stack1 = foundHelper || depth0.id;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "id", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\">\n                            <td><span><a class=\"simptip-position-top simptip-movable\" data-tooltip=\"";
    foundHelper = helpers.id;
    stack1 = foundHelper || depth0.id;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "id", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\" href=\"singularity/task/";
    foundHelper = helpers.id;
    stack1 = foundHelper || depth0.id;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "id", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\" data-route=\"task/";
    foundHelper = helpers.id;
    stack1 = foundHelper || depth0.id;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "id", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\">";
    foundHelper = helpers.name;
    stack1 = foundHelper || depth0.name;
    foundHelper = helpers.hardBreak;
    stack2 = foundHelper || depth0.hardBreak;
    tmp1 = self.program(2, program2, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack2, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "</span></a></td>\n                            <td><span>";
    foundHelper = helpers.host;
    stack1 = foundHelper || depth0.host;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "host", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</span></td>\n                            <td><span>";
    foundHelper = helpers.resources;
    stack1 = foundHelper || depth0.resources;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.cpus);
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "resources.cpus", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</span></td>\n                            <td><span>";
    foundHelper = helpers.resources;
    stack1 = foundHelper || depth0.resources;
    stack1 = (stack1 === null || stack1 === undefined || stack1 === false ? stack1 : stack1.memoryMb);
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "resources.memoryMb", { hash: {} }); }
    buffer += escapeExpression(stack1) + "Mb</span></td>\n                            <td><span class=\"ellipsis simptip-position-top simptip-movable\" data-tooltip=\"";
    foundHelper = helpers.startedAt;
    stack1 = foundHelper || depth0.startedAt;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "startedAt", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\">";
    foundHelper = helpers.startedAtHuman;
    stack1 = foundHelper || depth0.startedAtHuman;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "startedAtHuman", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</span></td>\n                            <td><span><a data-task-id=\"";
    foundHelper = helpers.id;
    stack1 = foundHelper || depth0.id;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "id", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\" class=\"dont-route view-json\">JSON</a></span><td>\n                        </tr>\n                    ";
    return buffer;}
  function program2(depth0,data) {
    
    var buffer = "";
    return buffer;}

  function program4(depth0,data) {
    
    var buffer = "", stack1, stack2;
    buffer += "\n                        <tr data-task-id=\"";
    foundHelper = helpers.id;
    stack1 = foundHelper || depth0.id;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "id", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\">\n                            <td><span class=\"simptip-position-top simptip-movable\" data-tooltip=\"";
    foundHelper = helpers.id;
    stack1 = foundHelper || depth0.id;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "id", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\">";
    foundHelper = helpers.name;
    stack1 = foundHelper || depth0.name;
    foundHelper = helpers.hardBreak;
    stack2 = foundHelper || depth0.hardBreak;
    tmp1 = self.program(5, program5, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack2, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "</span></td>\n                            <td><span class=\"ellipsis simptip-position-top simptip-movable\" data-tooltip=\"";
    foundHelper = helpers.nextRunAt;
    stack1 = foundHelper || depth0.nextRunAt;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "nextRunAt", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\">";
    foundHelper = helpers.nextRunAtHuman;
    stack1 = foundHelper || depth0.nextRunAtHuman;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "nextRunAtHuman", { hash: {} }); }
    buffer += escapeExpression(stack1) + "</span></td>\n                            <td><span><a data-task-id=\"";
    foundHelper = helpers.id;
    stack1 = foundHelper || depth0.id;
    if(typeof stack1 === functionType) { stack1 = stack1.call(depth0, { hash: {} }); }
    else if(stack1=== undef) { stack1 = helperMissing.call(depth0, "id", { hash: {} }); }
    buffer += escapeExpression(stack1) + "\" class=\"dont-route view-json\">JSON</a></span><td>\n                        </tr>\n                    ";
    return buffer;}
  function program5(depth0,data) {
    
    var buffer = "";
    return buffer;}

    buffer += "<header class=\"jumbotron subhead\" id=\"overview\">\n    <h1>Tasks</h1>\n</header>\n\n<section>\n    <!-- <input type=\"search\" placeholder=\"Search tasks...\" required /> -->\n    <div class=\"row-fluid\">\n        <div class=\"span12\">\n            <h2>Active Tasks</h2>\n            <table class=\"table\">\n                <thead>\n                    <tr>\n                        <th>Name</th>\n                        <th>Host</th>\n                        <th>CPUs</th>\n                        <th>Memory</th>\n                        <th>Started</th>\n                        <th>JSON</th>\n                    </tr>\n                </thead>\n                <tbody>\n                    ";
    foundHelper = helpers.tasksActive;
    stack1 = foundHelper || depth0.tasksActive;
    foundHelper = helpers.eachWithFn;
    stack2 = foundHelper || depth0.eachWithFn;
    tmp1 = self.program(1, program1, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack2, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n                </tbody>\n            </table>\n        </div>\n    </div>\n    <div class=\"row-fluid\">\n        <div class=\"span12\">\n            <h2>Scheduled Tasks</h2>\n            <table class=\"table\">\n                <thead>\n                    <tr>\n                        <th>Name</th>\n                        <th>Scheduled</th>\n                        <th>JSON</th>\n                    </tr>\n                </thead>\n                <tbody>\n                    ";
    foundHelper = helpers.tasksScheduled;
    stack1 = foundHelper || depth0.tasksScheduled;
    foundHelper = helpers.eachWithFn;
    stack2 = foundHelper || depth0.eachWithFn;
    tmp1 = self.program(4, program4, data);
    tmp1.hash = {};
    tmp1.fn = tmp1;
    tmp1.inverse = self.noop;
    if(foundHelper && typeof stack2 === functionType) { stack1 = stack2.call(depth0, stack1, tmp1); }
    else { stack1 = blockHelperMissing.call(depth0, stack2, stack1, tmp1); }
    if(stack1 || stack1 === 0) { buffer += stack1; }
    buffer += "\n                </tbody>\n            </table>\n        </div>\n    </div>\n</section>";
    return buffer;});
}});

window.require.define({"views/view": function(exports, require, module) {
  (function() {
    var View,
      __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
      __hasProp = Object.prototype.hasOwnProperty,
      __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

    require('lib/view_helper');

    View = (function(_super) {

      __extends(View, _super);

      function View() {
        this.routeLink = __bind(this.routeLink, this);
        View.__super__.constructor.apply(this, arguments);
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
          if (url.indexOf('/') === 0) url = url.substring(1);
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

  }).call(this);
  
}});

