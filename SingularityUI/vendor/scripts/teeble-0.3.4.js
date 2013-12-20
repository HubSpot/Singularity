/*!
* teeble - v0.3.4 - 2013-10-25
* https://github.com/HubSpot/teeble
* Copyright (c) 2013 HubSpot, Marc Neuwirth, Jonathan Kim;
* Licensed MIT
*/

(function() {

  this.Teeble = {};

}).call(this);

(function() {
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };

  this.Teeble.TableRenderer = (function() {

    TableRenderer.prototype.key = 'rows';

    TableRenderer.prototype.hasFooter = false;

    TableRenderer.prototype.data = null;

    TableRenderer.prototype.header_template = null;

    TableRenderer.prototype.row_template = null;

    TableRenderer.prototype.rows_template = null;

    TableRenderer.prototype.table_class = null;

    TableRenderer.prototype.table_template = null;

    TableRenderer.prototype.table_template_compiled = null;

    TableRenderer.prototype.empty_message = "No data to display";

    TableRenderer.prototype.classes = {
      sorting: {
        sortable_class: 'sorting'
      }
    };

    TableRenderer.prototype.compile = _.template;

    TableRenderer.prototype._initialize = function() {
      var option, validOptions, _i, _len;
      validOptions = ['table_class', 'partials', 'hasFooter', 'empty_message', 'cid', 'classes', 'compile'];
      for (_i = 0, _len = validOptions.length; _i < _len; _i++) {
        option = validOptions[_i];
        if (this.options[option] != null) {
          this[option] = this.options[option];
        }
      }
      if (this.partials) {
        return this.update_template(this.partials);
      }
    };

    TableRenderer.prototype._getExtraData = function() {
      return {};
    };

    TableRenderer.prototype._render = function(template, data) {
      if (!template) {
        console.log('no compiled template');
        return false;
      }
      if (!data) {
        console.log('no data');
        return false;
      } else {
        data = _.extend({}, this._getExtraData(), data);
        return template(data);
      }
    };

    function TableRenderer(options) {
      this.options = options;
      this.update_template = __bind(this.update_template, this);

      this.generate_columns = __bind(this.generate_columns, this);

      this.render_empty = __bind(this.render_empty, this);

      this.render_footer = __bind(this.render_footer, this);

      this.render_header = __bind(this.render_header, this);

      this.render_row = __bind(this.render_row, this);

      this._render = __bind(this._render, this);

      this._getExtraData = __bind(this._getExtraData, this);

      this._initialize = __bind(this._initialize, this);

      this._initialize();
      this;

    }

    TableRenderer.prototype.render_row = function(data) {
      if (!this.row_template_compiled) {
        this.row_template_compiled = this.compile(this.row_template);
      }
      if (data) {
        return this._render(this.row_template_compiled, data);
      }
    };

    TableRenderer.prototype.render_header = function(data) {
      if (!this.header_template_compiled) {
        this.header_template_compiled = this.compile(this.header_template);
      }
      if (data) {
        return this._render(this.header_template_compiled, data);
      }
    };

    TableRenderer.prototype.render_footer = function(data) {
      if (!this.footer_template_compiled) {
        this.footer_template_compiled = this.compile(this.footer_template);
      }
      if (data) {
        return this._render(this.footer_template_compiled, data);
      }
    };

    TableRenderer.prototype.render_empty = function(data) {
      if (!this.table_empty_template_compiled) {
        this.table_empty_template_compiled = this.compile(this.table_empty_template);
      }
      if (data) {
        if (!data.message) {
          data.message = this.empty_message;
        }
        return this._render(this.table_empty_template_compiled, data);
      }
    };

    TableRenderer.prototype._get_template_attributes = function(type, partial, i) {
      var attribute, attributes, section, sortable, template, value, wrap, _ref;
      sortable = partial.sortable;
      section = partial[type];
      wrap = false;
      if (typeof section === 'string') {
        template = section;
      } else {
        wrap = true;
        if (section.template) {
          template = section.template;
        }
        if (!section.attributes) {
          section.attributes = {};
        }
        if (sortable) {
          section.attributes['data-sort'] = sortable;
          if (!section.attributes["class"]) {
            section.attributes["class"] = [this.classes.sorting.sortable_class];
          } else {
            if (typeof section.attributes["class"] === 'string') {
              section.attributes["class"] = [section.attributes["class"]];
            }
            section.attributes["class"].push(this.classes.sorting.sortable_class);
          }
        }
        attributes = [];
        _ref = section.attributes;
        for (attribute in _ref) {
          value = _ref[attribute];
          if (value instanceof Array) {
            value = value.join(' ');
          }
          attributes.push({
            name: attribute,
            value: value
          });
        }
      }
      if (template) {
        return {
          attributes: attributes,
          wrap: wrap,
          partial: template
        };
      } else {
        return {
          attributes: {},
          wrap: wrap,
          partial: ''
        };
      }
    };

    TableRenderer.prototype._generate_template = function(name, columns, wrap, td) {
      var attribute, attributes, column, column_name, column_template, section, str, _i, _len, _ref, _ref1;
      if (td == null) {
        td = 'td';
      }
      str = "";
      if (columns) {
        for (column_name in columns) {
          column = columns[column_name];
          section = column[name];
          if (section) {
            column_template = "" + section.partial;
            if (section.wrap) {
              attributes = '';
              if ((_ref = section.attributes) != null ? _ref.length : void 0) {
                _ref1 = section.attributes;
                for (_i = 0, _len = _ref1.length; _i < _len; _i++) {
                  attribute = _ref1[_i];
                  attributes += "" + attribute.name + "=\"" + attribute.value + "\" ";
                }
              }
              column_template = "<" + td + " " + attributes + ">" + column_template + "</" + td + ">";
            }
            str += column_template;
          }
        }
        if (wrap) {
          str = "<" + wrap + ">" + str + "</" + wrap + ">";
        }
      }
      return str;
    };

    TableRenderer.prototype.generate_columns = function(partials, clear) {
      var column, i, partial, partial_name;
      if (partials == null) {
        partials = this.partials;
      }
      if (clear == null) {
        clear = false;
      }
      if (this.columns && !clear) {
        return this.columns;
      } else {
        i = 0;
        this.columns = [];
        for (partial_name in partials) {
          partial = partials[partial_name];
          column = {};
          /* Header
          */

          if (partial.header) {
            column.header = this._get_template_attributes('header', partial, i);
          }
          /* Footer
          */

          if (partial.footer) {
            column.footer = this._get_template_attributes('footer', partial, i);
          }
          /* Cell
          */

          if (partial.cell) {
            column.cell = this._get_template_attributes('cell', partial, i);
          }
          this.columns.push(column);
          i++;
        }
        return this.columns;
      }
    };

    TableRenderer.prototype.update_template = function(partials) {
      var columns;
      if (partials == null) {
        partials = this.partials;
      }
      columns = this.generate_columns();
      this.header_template = this._generate_template('header', columns, 'tr', 'th');
      this.footer_template = this._generate_template('footer', columns, 'tr');
      this.row_template = this._generate_template('cell', columns);
      return this.table_empty_template = "<td valign=\"top\" colspan=\"" + columns.length + "\" class=\"teeble_empty\">{{message}}</td>";
    };

    return TableRenderer;

  })();

}).call(this);

(function() {
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  this.Teeble.EmptyView = (function(_super) {

    __extends(EmptyView, _super);

    function EmptyView() {
      this.render = __bind(this.render, this);

      this.initialize = __bind(this.initialize, this);
      return EmptyView.__super__.constructor.apply(this, arguments);
    }

    EmptyView.prototype.initialize = function() {
      this.renderer = this.options.renderer;
      return this.collection.bind('destroy', this.remove, this);
    };

    EmptyView.prototype.render = function() {
      if (this.renderer) {
        this.el = this.renderer.render_empty(this.options);
      }
      return this;
    };

    return EmptyView;

  })(Backbone.View);

}).call(this);

(function() {
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  this.Teeble.FooterView = (function(_super) {

    __extends(FooterView, _super);

    function FooterView() {
      this.render = __bind(this.render, this);

      this.initialize = __bind(this.initialize, this);
      return FooterView.__super__.constructor.apply(this, arguments);
    }

    FooterView.prototype.tagName = 'tfoot';

    FooterView.prototype.initialize = function() {
      var _this = this;
      this.renderer = this.options.renderer;
      this.collection.bind('destroy', this.remove, this);
      if (this.collection.footer) {
        if (this.collection.footer instanceof Backbone.Model) {
          this.collection.footer.on('change', function() {
            return _this.render();
          });
        }
        this.data = this.collection.footer;
      } else {
        this.data = this.options;
      }
      return this.collection.footer;
    };

    FooterView.prototype.render = function() {
      var data;
      if (this.renderer) {
        if (this.data.toJSON) {
          data = this.data.toJSON();
        } else {
          data = this.data;
        }
        this.$el.html(this.renderer.render_footer(data));
      }
      return this;
    };

    return FooterView;

  })(Backbone.View);

}).call(this);

(function() {
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  this.Teeble.HeaderView = (function(_super) {

    __extends(HeaderView, _super);

    function HeaderView() {
      this.setSort = __bind(this.setSort, this);

      this.sort = __bind(this.sort, this);

      this._sort = __bind(this._sort, this);

      this.render = __bind(this.render, this);

      this.initialize = __bind(this.initialize, this);
      return HeaderView.__super__.constructor.apply(this, arguments);
    }

    HeaderView.prototype.events = {
      'click .sorting': 'sort'
    };

    HeaderView.prototype.tagName = 'thead';

    HeaderView.prototype.initialize = function() {
      this.renderer = this.options.renderer;
      this.classes = this.options.classes;
      this.collection.bind('destroy', this.remove, this);
      return this.collection.bind('reset', this.setSort, this);
    };

    HeaderView.prototype.render = function() {
      if (this.renderer) {
        this.$el.html(this.renderer.render_header(this.options));
        this.setSort();
      }
      return this;
    };

    HeaderView.prototype._sort = function(e, direction) {
      var $this, currentSort, _ref;
      e.preventDefault();
      $this = this.$(e.target);
      if (!$this.hasClass(this.classes.sorting.sortable_class)) {
        $this = $this.parents("." + this.classes.sorting.sortable_class);
      }
      currentSort = $this.attr('data-sort');
      if (!$this.hasClass(this.classes.sorting.sorted_desc_class) && !$this.hasClass(this.classes.sorting.sorted_asc_class)) {
        direction = (_ref = this.collection.sortDirections[currentSort]) != null ? _ref : direction;
      }
      return this.collection.setSort(currentSort, direction);
    };

    HeaderView.prototype.sort = function(e) {
      var $this;
      $this = this.$(e.currentTarget);
      if ($this.hasClass(this.classes.sorting.sorted_desc_class)) {
        return this._sort(e, 'asc');
      } else {
        return this._sort(e, 'desc');
      }
    };

    HeaderView.prototype.setSort = function() {
      var classDirection, direction;
      if (this.collection.sortColumn) {
        direction = 'desc';
        if (this.collection.sortDirection) {
          direction = this.collection.sortDirection;
        }
        classDirection = "sorted_" + direction + "_class";
        return this.$el.find("." + this.classes.sorting.sortable_class).removeClass("" + this.classes.sorting.sorted_desc_class + " " + this.classes.sorting.sorted_asc_class).filter(".sorting[data-sort=\"" + this.collection.sortColumn + "\"]").addClass("" + this.classes.sorting[classDirection]);
      }
    };

    return HeaderView;

  })(Backbone.View);

}).call(this);

(function() {
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  this.Teeble.PaginationView = (function(_super) {

    __extends(PaginationView, _super);

    function PaginationView() {
      this.gotoPage = __bind(this.gotoPage, this);

      this.gotoLast = __bind(this.gotoLast, this);

      this.gotoNext = __bind(this.gotoNext, this);

      this.gotoPrev = __bind(this.gotoPrev, this);

      this.gotoFirst = __bind(this.gotoFirst, this);

      this.render = __bind(this.render, this);

      this.initialize = __bind(this.initialize, this);
      return PaginationView.__super__.constructor.apply(this, arguments);
    }

    PaginationView.prototype.tagName = 'div';

    PaginationView.prototype.events = {
      'click a.first': 'gotoFirst',
      'click a.previous': 'gotoPrev',
      'click a.next': 'gotoNext',
      'click a.last': 'gotoLast',
      'click a.pagination-page': 'gotoPage'
    };

    PaginationView.prototype.template = "<div class=\" <%= pagination_class %>\">\n    <ul>\n        <li>\n            <a href=\"#\" class=\"pagination-previous previous <% if (prev_disabled){ %><%= pagination_disabled %><% } %>\">\n                <span class=\"left\"></span>\n                Previous\n            </a>\n        </li>\n        <% _.each(pages, function(page) { %>\n        <li>\n            <a href=\"#\" class=\"pagination-page <% if (page.active){ %><%= pagination_active %><% } %>\" data-page=\"<%= page.number %>\"><%= page.number %></a>\n        </li>\n        <% }); %>\n        <li>\n            <a href=\"#\" class=\"pagination-next next <% if(next_disabled){ %><%= pagination_disabled %><% } %>\">\n                Next\n                <span class=\"right\"></span>\n            </a>\n        </li>\n    </ul>\n</div>";

    PaginationView.prototype.initialize = function() {
      this.collection.bind('destroy', this.remove, this);
      return PaginationView.__super__.initialize.apply(this, arguments);
    };

    PaginationView.prototype.render = function() {
      var html, info, p, page, pages;
      if (!this.collection.information) {
        this.collection.pager();
      }
      info = this.collection.information;
      if (info.totalPages > 1) {
        pages = (function() {
          var _i, _len, _ref, _results;
          _ref = info.pageSet;
          _results = [];
          for (_i = 0, _len = _ref.length; _i < _len; _i++) {
            page = _ref[_i];
            p = {
              active: page === info.currentPage ? this.options.pagination.pagination_active : void 0,
              number: page
            };
            _results.push(p);
          }
          return _results;
        }).call(this);
        html = _.template(this.template, {
          pagination_class: this.options.pagination.pagination_class,
          pagination_disabled: this.options.pagination.pagination_disabled,
          pagination_active: this.options.pagination.pagination_active,
          prev_disabled: info.previous === false || info.hasPrevious === false,
          next_disabled: info.next === false || info.hasNext === false,
          pages: pages
        });
        this.$el.html(html);
      }
      return this;
    };

    PaginationView.prototype.gotoFirst = function(e) {
      e.preventDefault();
      return this.collection.goTo(1);
    };

    PaginationView.prototype.gotoPrev = function(e) {
      e.preventDefault();
      return this.collection.previousPage();
    };

    PaginationView.prototype.gotoNext = function(e) {
      e.preventDefault();
      return this.collection.nextPage();
    };

    PaginationView.prototype.gotoLast = function(e) {
      e.preventDefault();
      return this.collection.goTo(this.collection.information.lastPage);
    };

    PaginationView.prototype.gotoPage = function(e) {
      var page;
      e.preventDefault();
      page = this.$(e.target).text();
      return this.collection.goTo(page);
    };

    return PaginationView;

  })(Backbone.View);

}).call(this);

(function() {
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  this.Teeble.RowView = (function(_super) {

    __extends(RowView, _super);

    function RowView() {
      this.render = __bind(this.render, this);

      this.initialize = __bind(this.initialize, this);
      return RowView.__super__.constructor.apply(this, arguments);
    }

    RowView.prototype.tagName = 'tr';

    RowView.prototype.initialize = function() {
      this.renderer = this.options.renderer;
      this.model.bind('change', this.render, this);
      return this.model.bind('destroy', this.remove, this);
    };

    RowView.prototype.render = function() {
      if (this.renderer) {
        this.$el.html(this.renderer.render_row(this.model.toJSON({
          teeble: true
        })));
        if (this.options.sortColumnIndex != null) {
          this.$el.find('td').eq(this.options.sortColumnIndex).addClass(this.options.sortableClass);
        }
      }
      return this;
    };

    return RowView;

  })(Backbone.View);

}).call(this);

(function() {
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  this.Teeble.TableView = (function(_super) {

    __extends(TableView, _super);

    function TableView() {
      this.addOne = __bind(this.addOne, this);

      this.renderEmpty = __bind(this.renderEmpty, this);

      this.renderBody = __bind(this.renderBody, this);

      this.renderFooter = __bind(this.renderFooter, this);

      this.renderHeader = __bind(this.renderHeader, this);

      this.renderPagination = __bind(this.renderPagination, this);

      this.render = __bind(this.render, this);

      this.setOptions = __bind(this.setOptions, this);

      this.initialize = __bind(this.initialize, this);
      return TableView.__super__.constructor.apply(this, arguments);
    }

    TableView.prototype.tagName = 'div';

    TableView.prototype.rendered = false;

    TableView.prototype.classes = {
      sorting: {
        sortable_class: 'sorting',
        sorted_desc_class: 'sorting_desc',
        sorted_asc_class: 'sorting_asc',
        sortable_cell: 'sorting_1'
      },
      pagination: {
        pagination_class: 'pagination',
        pagination_active: 'active',
        pagination_disabled: 'disabled'
      }
    };

    TableView.prototype.subviews = {
      header: Teeble.HeaderView,
      row: Teeble.RowView,
      footer: Teeble.FooterView,
      pagination: Teeble.PaginationView,
      renderer: Teeble.TableRenderer,
      empty: Teeble.EmptyView
    };

    TableView.prototype.initialize = function() {
      var i, partial, partial_name, _ref;
      this.subviews = _.extend({}, this.subviews, this.options.subviews);
      this.setOptions();
      TableView.__super__.initialize.apply(this, arguments);
      this.collection.on('add', this.addOne, this);
      this.collection.on('reset', this.renderBody, this);
      this.collection.on('reset', this.renderPagination, this);
      this.sortIndex = {};
      i = 0;
      _ref = this.options.partials;
      for (partial_name in _ref) {
        partial = _ref[partial_name];
        if (partial.sortable) {
          this.sortIndex[partial.sortable] = i;
        }
        i++;
      }
      return this.renderer = new this.subviews.renderer({
        partials: this.options.partials,
        table_class: this.options.table_class,
        cid: this.cid,
        classes: this.classes,
        collection: this.collection,
        compile: this.options.compile
      });
    };

    TableView.prototype.setOptions = function() {
      return this;
    };

    TableView.prototype.render = function() {
      var _base;
      if (!this.collection.origModels && (this.collection.whereAll != null)) {
        if (typeof (_base = this.collection).pager === "function") {
          _base.pager();
        }
      }
      this.$el.empty().append("<table><tbody></tbody></table");
      this.table = this.$('table').addClass(this.options.table_class);
      this.body = this.$('tbody');
      this.rendered = true;
      this.renderHeader();
      this.renderBody();
      this.renderFooter();
      this.renderPagination();
      this.trigger('teeble.render', this);
      return this;
    };

    TableView.prototype.renderPagination = function() {
      var _ref;
      if (this.options.pagination && this.rendered) {
        if ((_ref = this.pagination) != null) {
          _ref.remove();
        }
        this.pagination = new this.subviews.pagination({
          collection: this.collection,
          pagination: this.classes.pagination
        });
        this.$el.append(this.pagination.render().el);
        return this.trigger('pagination.render', this);
      }
    };

    TableView.prototype.renderHeader = function() {
      var _ref;
      if (this.rendered) {
        if ((_ref = this.header) != null) {
          _ref.remove();
        }
        this.header = new this.subviews.header({
          renderer: this.renderer,
          collection: this.collection,
          classes: this.classes
        });
        this.table.prepend(this.header.render().el);
        return this.trigger('header.render', this);
      }
    };

    TableView.prototype.renderFooter = function() {
      var _ref;
      if (this.options.footer && this.rendered) {
        if ((_ref = this.footer) != null) {
          _ref.remove();
        }
        if (this.collection.length > 0) {
          this.footer = new this.subviews.footer({
            renderer: this.renderer,
            collection: this.collection
          });
          this.table.append(this.footer.render().el);
          return this.trigger('footer.render', this);
        }
      }
    };

    TableView.prototype.renderBody = function() {
      if (this.rendered) {
        this.body.empty();
        if (this.collection.length > 0) {
          this.collection.each(this.addOne);
          return this.trigger('body.render', this);
        } else {
          return this.renderEmpty();
        }
      }
    };

    TableView.prototype.renderEmpty = function() {
      var options;
      if (this.rendered) {
        options = _.extend({}, this.options, {
          renderer: this.renderer,
          collection: this.collection
        });
        this.empty = new this.subviews.empty(options);
        this.body.append(this.empty.render().el);
        return this.trigger('empty.render', this);
      }
    };

    TableView.prototype.addOne = function(item) {
      var sortColumnIndex, view;
      if (this.collection.sortColumn) {
        sortColumnIndex = this.sortIndex[this.collection.sortColumn];
      }
      view = new this.subviews.row({
        model: item,
        renderer: this.renderer,
        sortColumnIndex: sortColumnIndex,
        sortableClass: this.classes.sorting.sortable_cell
      });
      this.body.append(view.render().el);
      return this.trigger('row.render', view);
    };

    return TableView;

  })(Backbone.View);

}).call(this);

(function() {
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; },
    __slice = [].slice;

  this.Teeble.ClientCollection = (function(_super) {

    __extends(ClientCollection, _super);

    function ClientCollection() {
      this.eachAll = __bind(this.eachAll, this);

      this.getFromAll = __bind(this.getFromAll, this);

      this.whereAll = __bind(this.whereAll, this);

      this.initialize = __bind(this.initialize, this);
      return ClientCollection.__super__.constructor.apply(this, arguments);
    }

    ClientCollection.prototype.sortDirections = {};

    ClientCollection.prototype.default_paginator_core = {
      dataType: 'json',
      url: function() {
        return this.url();
      }
    };

    ClientCollection.prototype.default_paginator_ui = {
      sortColumn: '',
      sortDirection: 'desc',
      firstPage: 1,
      currentPage: 1,
      perPage: 10,
      pagesInRange: 3
    };

    ClientCollection.prototype.initialize = function() {
      this.paginator_ui = _.extend({}, this.default_paginator_ui, this.paginator_ui);
      this.paginator_core = _.extend({}, this.default_paginator_core, this.paginator_core);
      return ClientCollection.__super__.initialize.apply(this, arguments);
    };

    ClientCollection.prototype.whereAll = function(attrs) {
      if (_.isEmpty(attrs)) {
        return [];
      }
      return _.filter(this.origModels, function(model) {
        var key, value;
        for (key in attrs) {
          value = attrs[key];
          if (value !== model.get(key)) {
            return false;
          }
        }
        return true;
      });
    };

    ClientCollection.prototype.getFromAll = function(obj) {
      var id;
      if (obj == null) {
        return void 0;
      }
      id = obj.id || obj.cid || obj;
      return this._byId[id] || _.findWhere(this.origModels, {
        id: id
      });
    };

    ClientCollection.prototype.eachAll = function() {
      var _ref, _ref1;
      return (_ref1 = _.each).call.apply(_ref1, [_, (_ref = this.origModels) != null ? _ref : this.models].concat(__slice.call(arguments)));
    };

    return ClientCollection;

  })(Backbone.Paginator.clientPager);

}).call(this);

(function() {
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  this.Teeble.ServerCollection = (function(_super) {

    __extends(ServerCollection, _super);

    function ServerCollection() {
      this.pager = __bind(this.pager, this);

      this.setSort = __bind(this.setSort, this);

      this.previousPage = __bind(this.previousPage, this);

      this.nextPage = __bind(this.nextPage, this);

      this.initialize = __bind(this.initialize, this);
      return ServerCollection.__super__.constructor.apply(this, arguments);
    }

    ServerCollection.prototype.sortDirections = {};

    ServerCollection.prototype.default_paginator_core = {
      dataType: 'json',
      url: function() {
        return this.url();
      }
    };

    ServerCollection.prototype.default_paginator_ui = {
      firstPage: 1,
      currentPage: 1,
      perPage: 10,
      pagesInRange: 3
    };

    ServerCollection.prototype.default_server_api = {
      'offset': function() {
        return (this.currentPage - 1) * this.perPage;
      },
      'limit': function() {
        return this.perPage;
      }
    };

    ServerCollection.prototype.initialize = function() {
      this.paginator_ui = _.extend({}, this.default_paginator_ui, this.paginator_ui);
      this.paginator_core = _.extend({}, this.default_paginator_core, this.paginator_core);
      this.server_api = _.extend({}, this.default_server_api, this.server_api);
      this.on('reset', this.info);
      return ServerCollection.__super__.initialize.apply(this, arguments);
    };

    ServerCollection.prototype.nextPage = function(options) {
      if (this.currentPage < this.information.totalPages) {
        return this.promise = this.requestNextPage(options);
      }
    };

    ServerCollection.prototype.previousPage = function(options) {
      if (this.currentPage > 1) {
        return this.promise = this.requestPreviousPage(options);
      }
    };

    ServerCollection.prototype.setSort = function(column, direction) {
      if (column !== void 0 && direction !== void 0) {
        this.lastSortColumn = this.sortColumn;
        this.sortColumn = column;
        this.sortDirection = direction;
        this.pager();
        return this.info();
      }
    };

    ServerCollection.prototype.pager = function() {
      if (this.lastSortColumn !== this.sortColumn && (this.sortColumn != null)) {
        this.currentPage = 1;
        this.lastSortColumn = this.sortColumn;
      }
      ServerCollection.__super__.pager.apply(this, arguments);
      return this.info();
    };

    return ServerCollection;

  })(Backbone.Paginator.requestPager);

}).call(this);

(function() {
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  this.Teeble.SortbarHeaderView = (function(_super) {

    __extends(SortbarHeaderView, _super);

    function SortbarHeaderView() {
      this.sort = __bind(this.sort, this);

      this.sortFieldChange = __bind(this.sortFieldChange, this);

      this.sortBarChange = __bind(this.sortBarChange, this);
      return SortbarHeaderView.__super__.constructor.apply(this, arguments);
    }

    SortbarHeaderView.prototype.events = {
      'change .sortbar-column': 'sortBarChange',
      'change .sortbar-field-select': 'sortFieldChange',
      'click .sort-reverser': 'sort'
    };

    SortbarHeaderView.prototype.sortBarChange = function(e) {
      var $this, column, existing, oldValue, value;
      $this = this.$(e.currentTarget);
      column = ~~($this.attr('data-column'));
      value = $this.val();
      oldValue = this.collection.sortbarColumns[column];
      existing = _.indexOf(this.collection.sortbarColumns, value);
      if (existing >= 0) {
        this.collection.sortbarColumns[existing] = oldValue;
      }
      this.collection.sortbarColumns[column] = value;
      this.renderer.update_template();
      this.render();
      return this.collection.trigger('reset');
    };

    SortbarHeaderView.prototype.sortFieldChange = function(e) {
      return this.sort(e, 'asc');
    };

    SortbarHeaderView.prototype.sort = function(e, direction) {
      var $sortReverser, currentSort;
      if (e != null) {
        e.preventDefault();
      }
      $sortReverser = this.$('.sort-reverser');
      if ($sortReverser.hasClass('reverse') || direction) {
        $sortReverser.removeClass('reverse');
        direction = 'asc';
      } else {
        $sortReverser.addClass('reverse');
        direction = 'desc';
      }
      currentSort = this.$('.sortbar-field-select').val();
      return this.collection.setSort(currentSort, direction);
    };

    return SortbarHeaderView;

  })(this.Teeble.HeaderView);

}).call(this);

(function() {
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; },
    __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  this.Teeble.SortbarRenderer = (function(_super) {

    __extends(SortbarRenderer, _super);

    function SortbarRenderer() {
      this.update_template = __bind(this.update_template, this);

      this._generate_template = __bind(this._generate_template, this);

      this._getExtraData = __bind(this._getExtraData, this);
      return SortbarRenderer.__super__.constructor.apply(this, arguments);
    }

    SortbarRenderer.prototype._getExtraData = function() {
      return {
        sortbarColumns: this.options.collection.sortbarColumns,
        sortbarSortOptions: this.options.collection.sortbarSortOptions,
        sortbarColumnOptions: this.options.collection.sortbarColumnOptions,
        sortColumn: this.options.collection.sortColumn,
        sortDirection: this.options.collection.sortDirection,
        partials: this.partials
      };
    };

    SortbarRenderer.prototype._generate_template = function(name, columns, wrap) {
      var column, str, _i, _len, _ref;
      str = SortbarRenderer.__super__._generate_template.call(this, name, columns);
      _ref = this.options.collection.sortbarColumns;
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        column = _ref[_i];
        str += "<td><%= " + column + " %></td>";
      }
      if (wrap) {
        str = "<" + wrap + ">" + str + "</" + wrap + ">";
      }
      return str;
    };

    SortbarRenderer.prototype.update_template = function(partials) {
      var columns;
      if (partials == null) {
        partials = this.partials;
      }
      columns = this.generate_columns();
      this.header_template = "<tr>\n    <th colspan=\"" + (_.size(partials)) + "\">\n        <div class=\"sort-label\">Sorted by: </div>\n        <div class=\"sort\">\n            <select class=\"sortbar-field-select\">\n                <% _.each(sortbarSortOptions, function(name, value) { %>\n                    <option value=\"<%= value %>\" <% if (sortColumn === value){ %>selected<% } %>><%= name %></option>\n                <% }); %>\n                <% _.each(sortbarColumnOptions, function(name, value) { %>\n                    <option value=\"<%= value %>\" <% if (sortColumn === value){ %>selected<% } %>><%= name %></option>\n                <% }); %>\n            </select>\n        </div>\n        <div class=\"sort-reverser <% if( sortDirection === 'desc' ){ %>reverse<% } %>\">\n            <div class=\"up\"></div>\n            <div class=\"down\"></div>\n        </div>\n         <div class=\"columns-label\">Showing:</div>\n    </th>\n    <% for(var i = 0; i < sortbarColumns.length; i++) { %>\n        <th>\n            <select data-column=\"<%= i %>\" class=\"sortbar-column sortbar-column-<%= i %>\">\n            <% _.each(sortbarColumnOptions, function(name, value) { %>\n                <option value=\"<%= value %>\" <% if(value === sortbarColumns[i]){%>selected<%}%> ><%= name %></option>\n            <% }); %>\n        </th>\n    <% } %>\n</tr>";
      this.footer_template = this._generate_template('footer', columns, 'tr');
      this.row_template = this._generate_template('cell', columns);
      this.table_empty_template = "<td valign=\"top\" colspan=\"" + columns.length + "\" class=\"teeble_empty\">{{message}}</td>";
      this.row_template_compiled = null;
      this.header_template_compiled = null;
      this.footer_template_compiled = null;
      return this.table_empty_template_compiled = null;
    };

    return SortbarRenderer;

  })(this.Teeble.TableRenderer);

}).call(this);