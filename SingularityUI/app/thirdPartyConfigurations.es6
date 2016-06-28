import vex from 'vex';
import moment from 'moment';
import Handlebars from 'handlebars';
import Messenger from 'messenger';

// Set Vex default className
vex.defaultOptions.className = 'vex-theme-default';

// Time out requests within 10 seconds
$.ajaxSetup(
  {timeout: 10 * 1000}
);

// Patch jQuery ajax to always use xhrFields.withCredentials true
let _oldAjax = jQuery.ajax;
jQuery.ajax = function(opts) {
  if (opts.xhrFields == null) { opts.xhrFields = {}; }
  opts.xhrFields.withCredentials = true;

  return _oldAjax.call(jQuery, opts);
};

// Eat M/D/Y & 24h-time, yanks! Mwahahahahaha!
moment.locale('en', {
  longDateFormat: {
    LT : 'HH:mm',
    L : 'DD/MM/YYYY',
    LL : 'D MMMM YYYY',
    LLL : 'D MMMM YYYY LT',
    LLLL : 'dddd, D MMMM YYYY LT'
  }
});

// Messenger options
Messenger.options = {
  extraClasses: 'messenger-fixed messenger-on-top',
  theme: 'air',
  hideOnNavigate: true,
  maxMessages: 1,
  messageDefaults: {
    type: 'error',
    hideAfter: false,
    showCloseButton: true
  }
};

// Overwrite Handlebars logging
Handlebars.logger.log = (...stuff) => {
  return stuff.slice(1).map(n => console.log(n)); //eslint-disable-line no-console
};
