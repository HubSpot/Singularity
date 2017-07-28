import moment from 'moment';
import 'messenger/build/css/messenger';
import 'messenger/build/css/messenger-theme-air';
import 'messenger';

export const loadThirdParty = () => {
  // Eat M/D/Y & 24h-time, yanks! Mwahahahahaha!
  moment.updateLocale('en', {
    longDateFormat: {
      LT: 'HH:mm',
      L: 'DD/MM/YYYY',
      LL: 'D MMMM YYYY',
      LLL: 'D MMMM YYYY LT',
      LLLL: 'dddd, D MMMM YYYY LT'
    }
  });

  // Time out requests within 10 seconds
  $.ajaxSetup(
    {timeout: 10 * 1000}
  );

  // Patch jQuery ajax to always use xhrFields.withCredentials true
  const _oldAjax = jQuery.ajax;
  jQuery.ajax = (opts) => {
    if (opts.xhrFields == null) { opts.xhrFields = {}; }
    opts.xhrFields.withCredentials = true;

    return _oldAjax.call(jQuery, opts);
  };

  // Messenger options
  window.Messenger.options = {
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
};

// Color scheme for json view
export const JSONTreeTheme = {
  base00: '#1e1e1e',
  base01: '#323537',
  base02: '#464b50',
  base03: '#5f5a60',
  base04: '#838184',
  base05: '#a7a7a7',
  base06: '#c3c3c3',
  base07: '#ffffff',
  base08: '#cf6a4c',
  base09: '#cda869',
  base0A: '#f9ee98',
  base0B: '#8f9d6a',
  base0C: '#afc4db',
  base0D: '#7587a6',
  base0E: '#9b859d',
  base0F: '#9b703f'
};
