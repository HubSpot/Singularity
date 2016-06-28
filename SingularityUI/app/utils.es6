let Clipboard;
let micromatch;
let vex;
const indexOf = [].indexOf || function(item) { for (let i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; };

Clipboard = require('clipboard');

vex = require('vex.dialog').default;

micromatch = require('micromatch');

let Utils = {
  TERMINAL_TASK_STATES: ['TASK_KILLED', 'TASK_LOST', 'TASK_FAILED', 'TASK_FINISHED', 'TASK_ERROR'],

  DECOMMISION_STATES: ['DECOMMISSIONING', 'DECOMMISSIONED', 'STARTING_DECOMMISSION', 'DECOMISSIONING', 'DECOMISSIONED', 'STARTING_DECOMISSION'],

  GLOB_CHARS: ['*', '!', '?', '[', ']'],

  viewJSON(model, callback) {
    let ajaxRequest, closeButton, copyButton, j, json, key, len, modelJSON, objectToSerialise, ref;
    if (model == null) {
      if (typeof callback === "function") {
        callback({
          error: 'Invalid model given'
        });
      }
      console.error('Invalid model given');
      return;
    }
    if ((model.synced != null) && !model.synced) {
      vex.showLoading();
      ajaxRequest = model.fetch();
      ajaxRequest.done(((_this => () => _this.viewJSON(model)))(this));
      ajaxRequest.error(((_this => () => {
        app.caughtError();
        return _this.viewJSON(new Backbone.Model({
          message: "There was an error with the server"
        }));
      }))(this));
      return;
    }
    vex.hideLoading();
    if (model.ignoreAttributes == null) {
      model.ignoreAttributes = ['id'];
    }
    objectToSerialise = {};
    modelJSON = model.toJSON();
    ref = _.keys(modelJSON);
    for (j = 0, len = ref.length; j < len; j++) {
      key = ref[j];
      if (indexOf.call(model.ignoreAttributes, key) < 0) {
        objectToSerialise[key] = modelJSON[key];
      }
    }
    json = JSON.stringify(objectToSerialise, void 0, 4);
    closeButton = _.extend(_.clone(vex.dialog.buttons.YES), {
      text: 'Close'
    });
    copyButton = {
      text: "Copy",
      type: "button",
      className: "vex-dialog-button-secondary copy-button"
    };
    return vex.dialog.open({
      buttons: [closeButton, copyButton],
      message: `<pre>${_.escape(json)}</pre>`,
      className: 'vex vex-theme-default json-modal',
      afterOpen($vexContent) {
        let $button, clipboard;
        $vexContent.parents('.vex').scrollTop(0);
        $button = $vexContent.find(".copy-button");
        $button.attr("data-clipboard-text", $vexContent.find("pre").html());
        return clipboard = new Clipboard($button[0]);
      }
    });
  },

  setupCopyLinks($element) {
    let $items;
    $items = $element.find(".horizontal-description-list li");
    return _.each($items, $item => {
      let $copyLink, text;
      $item = $($item);
      if (!$item.find('a').length) {
        text = $item.find('p').html();
        $copyLink = $(`<a data-clipboard-text='${_.escape(text)}'>Copy</a>`);
        $item.find("h4").append($copyLink);
        return new Clipboard($copyLink[0]);
      }
    });
  },

  makeMeCopy(options) {
    let $copyLink, $element, linkText, text, textSelector;
    $element = $(options.selector);
    linkText = options.linkText || 'Copy';
    textSelector = options.textSelector || '.copy-text';
    text = $element.find(textSelector).html();
    $copyLink = $(`<a data-clipboard-text='${_.escape(text)}'>${linkText}</a>`);
    $(options.copyLink).html($copyLink);
    return new Clipboard($copyLink[0]);
  },

  fixTableColumns($table) {
    let $heading, $headings, j, len, percentage, sortable, totalWidth;
    $headings = $table.find("th");
    if ($headings.length && $table.css('table-layout') !== 'fixed') {
      $table.css("table-layout", "auto");
      $headings.css("width", "auto");
      totalWidth = $table.width();
      sortable = $table.attr('data-sortable') !== void 0;
      if (!sortable) {
        for (j = 0, len = $headings.length; j < len; j++) {
          $heading = $headings[j];
          $heading = $($heading);
          percentage = $heading.width() / totalWidth * 100;
          $heading.css("width", `${percentage}%`);
        }
        return $table.css("table-layout", "fixed");
      }
    }
  },

  pathToBreadcrumbs(path) {
    let pathComponents, results;
    if (path == null) {
      path = "";
    }
    pathComponents = path.split('/');
    results = _.map(pathComponents, ((_this => (crumb, index) => {
      path = _.first(pathComponents, index);
      path.push(crumb);
      return {
        name: crumb,
        path: path.join('/')
      };
    }))(this));
    results.unshift({
      name: "root",
      path: ""
    });
    return results;
  },

  animatedExpansion($el, shrinkCallback) {
    let checkForShrink, newHeight, offset, removeEvent, scroll, shrink, shrinkTime;
    newHeight = $(window).height();
    offset = $el.offset().top;
    $('body').css('min-height', `${offset + newHeight}px`);
    $el.css('min-height', `${$el.height()}px`);
    $el.animate({
      duration: 1000,
      minHeight: `${newHeight}px`
    });
    scroll = ((_this => () => $(window).scrollTop($el.offset().top - 20)))(this);
    scroll();
    setTimeout(scroll, 200);
    shrinkTime = 1000;
    removeEvent = ((_this => () => $(window).off('scroll', checkForShrink)))(this);
    shrink = ((_this => () => {
      $('html, body').animate({
        minHeight: '0px'
      }, shrinkTime);
      $el.animate({
        minHeight: '0px'
      }, shrinkTime);
      if (typeof shrinkCallback === "function") {
        shrinkCallback();
      }
      return removeEvent();
    }))(this);
    checkForShrink = ((_this => () => {
      let frameRequest;
      if (typeof frameRequest !== "undefined" && frameRequest !== null) {
        cancelAnimationFrame(frameRequest);
      }
      return frameRequest = requestAnimationFrame(() => {
        let $window, childScrollBottom, elOffset, lastChild, scrolledEnoughBottom, scrolledEnoughTop, shouldShrink, windowScrollBottom, windowScrollTop;
        if (!$el) {
          removeEvent();
        }
        $window = $(window);
        lastChild = $(_.last($el.children()));
        if (!lastChild) {
          shrink();
        }
        elOffset = $el.offset().top;
        childScrollBottom = lastChild.height() + lastChild.offset().top;
        windowScrollTop = $window.scrollTop();
        windowScrollBottom = $window.height() + windowScrollTop;
        scrolledEnoughTop = windowScrollTop < elOffset - 50;
        scrolledEnoughBottom = windowScrollTop > elOffset + 50;
        scrolledEnoughBottom = scrolledEnoughBottom && windowScrollBottom > childScrollBottom;
        shouldShrink = scrolledEnoughTop || scrolledEnoughBottom;
        if (shouldShrink) {
          return shrink();
        }
      });
    }))(this);
    return setTimeout(((_this => () => {
      $(window).on('scroll', checkForShrink);
      return $el.on('shrink', shrink);
    }))(this), 100);
  },

  scrollTo(path, offset) {
    let location;
    if (offset == null) {
      offset = 50;
    }
    location = $(`${path}`).offset().top - offset;
    return $('html, body').animate({
      'scrollTop': `${location}px`
    }, 1000);
  },

  getQueryParams() {
    if (location.search) {
      return JSON.parse(`{"${decodeURI(location.search.substring(1).replace(/&/g, "\",\"").replace(/\=/g, "\":\""))}"}`);
    } else {
      return {};
    }
  },

  humanizeText(text) {
    if (!text) {
      return '';
    }
    text = text.replace(/_/g, ' ');
    text = text.toLowerCase();
    text = text[0].toUpperCase() + text.substr(1);
    return text;
  },

  humanizeFileSize(bytes) {
    let i, k, sizes;
    k = 1024;
    sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
    if (bytes === 0) {
      return '0 B';
    }
    i = Math.min(Math.floor(Math.log(bytes) / Math.log(k)), sizes.length - 1);
    return `${+(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`;
  },

  substituteTaskId(value, taskId) {
    return value.replace('$TASK_ID', taskId);
  },

  getLabelClassFromTaskState(state) {
    switch (state) {
      case 'TASK_STARTING':
      case 'TASK_CLEANING':
        return 'warning';
      case 'TASK_STAGING':
      case 'TASK_LAUNCHED':
      case 'TASK_RUNNING':
        return 'info';
      case 'TASK_FINISHED':
        return 'success';
      case 'TASK_LOST':
      case 'TASK_FAILED':
      case 'TASK_LOST_WHILE_DOWN':
      case 'TASK_ERROR':
        return 'danger';
      case 'TASK_KILLED':
        return 'default';
      default:
        return 'default';
    }
  },

  fileName(filePath) {
    return filePath.substring(filePath.lastIndexOf('/') + 1);
  },

  isGlobFilter(filter) {
    let char, j, len, ref;
    ref = this.GLOB_CHARS;
    for (j = 0, len = ref.length; j < len; j++) {
      char = ref[j];
      if (filter.indexOf(char) !== -1) {
        return true;
      }
    }
    return false;
  },

  fuzzyAdjustScore(filter, fuzzyObject) {
    if (fuzzyObject.original.request.id.toLowerCase().startsWith(filter.toLowerCase())) {
      return fuzzyObject.score * 10;
    } else if (fuzzyObject.original.request.id.toLowerCase().indexOf(filter.toLowerCase()) > -1) {
      return fuzzyObject.score * 5;
    } else {
      return fuzzyObject.score;
    }
  },

  getTaskDataFromTaskId(taskId) {
    let splits;
    splits = taskId.split('-');
    return {
      id: taskId,
      rackId: splits[splits.length - 1],
      host: splits[splits.length - 2],
      instanceNo: splits[splits.length - 3],
      startedAt: splits[splits.length - 4],
      deployId: splits[splits.length - 5],
      requestId: splits.slice(0, +(splits.length - 6) + 1 || 9e9).join('-')
    };
  },

  ignore404(response) {
    if (response.status === 404) {
      return app.caughtError();
    }
  },

  ignore400(response) {
    if (response.status === 400) {
      return app.caughtError();
    }
  }
};

export default Utils;
