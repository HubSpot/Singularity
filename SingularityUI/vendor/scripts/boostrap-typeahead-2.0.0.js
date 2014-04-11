//  ----------------------------------------------------------------------------
//
//  bootstrap-typeahead.js
//
//  Twitter Bootstrap Typeahead Plugin
//  v2.0.0
//  https://github.com/tcrosen/twitter-bootstrap-typeahead
//
//
//  Author
//  ----------
//  Terry Rosen
//  tcrosen@gmail.com | @rerrify
//
//
//  Description
//  ----------
//  Custom implementation of Twitter's Bootstrap Typeahead Plugin
//  http://twitter.github.com/bootstrap/javascript.html#typeahead
//
//
//  Requirements
//  ----------
//  jQuery 1.7+
//  Twitter Bootstrap 2.0+
//
//  ----------------------------------------------------------------------------

!
function ($) {

    "use strict";

    var _defaults = {
        source: [],
        maxResults: 8,
        minLength: 1,
        menu: '<ul class="typeahead dropdown-menu"></ul>',
        item: '<li><a href="#"></a></li>',
        display: 'name',
        val: 'id',
        itemSelected: function () { }
      },

      _keyCodes = {
        DOWN: 40,
        ENTER: 13 || 108,
        ESCAPE: 27,
        TAB: 9,
        UP: 38
      },

      Typeahead = function (element, options) {
          this.$element = $(element);
          this.options = $.extend(true, {}, $.fn.typeahead.defaults, options);
          this.$menu = $(this.options.menu).appendTo('body');
          this.sorter = this.options.sorter || this.sorter;
          this.highlighter = this.options.highlighter || this.highlighter;
          this.shown = false;
          this.initSource();
          this.listen();
      }

    Typeahead.prototype = {

        constructor: Typeahead,

        initSource: function() {
          if (this.options.source) {
            if (typeof this.options.source === 'string') {
              this.source = $.extend({}, $.ajaxSettings, { url: this.options.source })
            }
            else if (typeof this.options.source === 'object') {
              if (this.options.source instanceof Array) {
                this.source = this.options.source;
              } else {
                this.source = $.extend({}, $.ajaxSettings, this.options.source);
              }
            }
          }
        },

        eventSupported: function(eventName) {
          var isSupported = (eventName in this.$element);

          if (!isSupported) {
            this.$element.setAttribute(eventName, 'return;');
            isSupported = typeof this.$element[eventName] === 'function';
          }

          return isSupported;
        },

        lookup: function (event) {
          var that = this,
              items;

          this.query = this.$element.val();
          if (!this.query || this.query.length < this.options.minLength) {
            return this.shown ? this.hide() : this;
          }

          if (this.source.url) {
            if (this.xhr) this.xhr.abort();

            this.xhr = $.ajax(
              $.extend({}, this.source, {
                data: { query: that.query },
                success: $.proxy(that.filter, that)
              })
            );

          } else {
            items = $.proxy(that.filter(that.source), that);
          }
        },

        filter: function(data) {
          var that = this,
              items;

          items = $.grep(data, function (item) {
            return ~item[that.options.display].toLowerCase().indexOf(that.query.toLowerCase());
          });

          if (!items || !items.length) {
            return this.shown ? this.hide() : this;
          } else {
            items = items.slice(0, this.options.maxResults);
          }

          return this.render(this.sorter(items)).show();
        },

        sorter: function (items) {
          var that = this,
              beginswith = [],
              caseSensitive = [],
              caseInsensitive = [],
              item;

          while (item = items.shift()) {
            if (!item[that.options.display].toLowerCase().indexOf(this.query.toLowerCase())) {
              beginswith.push(item);
            } else if (~item[that.options.display].indexOf(this.query)) {
              caseSensitive.push(item);
            } else {
              caseInsensitive.push(item);
            }
          }

          return beginswith.concat(caseSensitive, caseInsensitive);
        },

        show: function () {
          var pos = $.extend({}, this.$element.offset(), {
              height: this.$element[0].offsetHeight
          });

          this.$menu.css({
              top: pos.top + pos.height,
              left: pos.left
          });

          this.$menu.show();
          this.shown = true;
          return this;
        },

        hide: function () {
          this.$menu.hide();
          this.shown = false;
          return this;
        },

        highlighter: function (item) {
          var query = this.query.replace(/[\-\[\]{}()*+?.,\\\^$|#\s]/g, '\\$&');
          return item.replace(new RegExp('(' + query + ')', 'ig'), function ($1, match) {
            return '<strong>' + match + '</strong>';
          });
        },

        render: function (items) {
          var that = this;

          items = $(items).map(function (i, item) {
              if (that.options.tmpl) {
                i = $(that.options.tmpl(item));
              } else {
                i = $(that.options.item);
              }

              if (typeof that.options.val === 'string') {
                i.attr('data-value', item[that.options.val]);
              }

              i.find('a').html(that.highlighter(item[that.options.display], item));
              return i[0];
          });

          items.first().addClass('active');
          this.$menu.html(items);
          return this;
        },

        select: function () {
          var $selectedItem = this.$menu.find('.active');
          this.$element.val($selectedItem.text()).change();
          this.options.itemSelected($selectedItem.attr('data-value'));
          return this.hide();
        },

        next: function (event) {
          var active = this.$menu.find('.active').removeClass('active');
          var next = active.next();

          if (!next.length) {
            next = $(this.$menu.find('li')[0]);
          }

          next.addClass('active');
        },

        prev: function (event) {
          var active = this.$menu.find('.active').removeClass('active');
          var prev = active.prev();

          if (!prev.length) {
            prev = this.$menu.find('li').last();
          }

          prev.addClass('active');
        },

        listen: function () {
            this.$element
              .on('blur', $.proxy(this.blur, this))
              .on('keypress', $.proxy(this.keypress, this))
              .on('keyup', $.proxy(this.keyup, this));

            if (this.eventSupported('keydown')) {
              this.$element
                .on('keydown', $.proxy(this.keypress, this));
            }

            this.$menu
              .on('click', $.proxy(this.click, this))
              .on('mouseenter', 'li', $.proxy(this.mouseenter, this));
        },

        keyup: function (e) {
          e.stopPropagation();
          e.preventDefault();

          switch (e.keyCode) {
            case _keyCodes.DOWN:
            case _keyCodes.UP:
               break;
            case _keyCodes.TAB:
            case _keyCodes.ENTER:
              if (!this.shown) return;
              this.select();
              break;
            case _keyCodes.ESCAPE:
              this.hide();
              break;
            default:
              this.lookup();
          }
        },

        keypress: function (e) {
          e.stopPropagation();

          if (!this.shown) return;

          switch (e.keyCode) {
            case _keyCodes.TAB:
            case _keyCodes.ESCAPE:
            case _keyCodes.ENTER:
              e.preventDefault();
              break;
            case _keyCodes.UP:
              e.preventDefault();
              this.prev();
              break;
            case _keyCodes.DOWN:
              e.preventDefault();
              this.next();
              break;
          }
        },

        blur: function (e) {
          var that = this;
          e.stopPropagation();
          e.preventDefault();
          setTimeout(function () {
            if (!that.$menu.is(':focus')) {
              that.hide();
            }
          }, 150);
        },

        click: function (e) {
          e.stopPropagation();
          e.preventDefault();
          this.select();
        },

        mouseenter: function (e) {
          this.$menu.find('.active').removeClass('active');
          $(e.currentTarget).addClass('active');
        }
    }

    //  Plugin definition
    $.fn.typeahead = function (option) {
      return this.each(function () {
        var $this = $(this),
            data = $this.data('typeahead'),
            options = typeof option === 'object' && option;

        if (!data) {
            $this.data('typeahead', (data = new Typeahead(this, options)));
        }

        if (typeof option === 'string') {
            data[option]();
        }
      });
    }

    $.fn.typeahead.defaults = _defaults;
    $.fn.typeahead.Constructor = Typeahead;

    //  Data API (no-JS implementation)
  $(function () {
      $('body').on('focus.typeahead.data-api', '[data-provide="typeahead"]', function (e) {
        var $this = $(this);
        if ($this.data('typeahead')) return;
        e.preventDefault();
        $this.typeahead($this.data());
      })
    });
} (window.jQuery);