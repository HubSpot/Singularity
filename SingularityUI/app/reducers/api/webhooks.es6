import * as WebhookActions from '../../actions/api/webhooks';

const initialState = {
  isFetching: false,
  error: null,
  receivedAt: null,
  data: []
};

export default function webhooks(state = initialState, action) {
  switch (action.type) {
    case WebhookActions.FETCH_WEBHOOKS_ERROR:
      return Object.assign({}, state, {
        isFetching: false,
        error: action.error
      });
    case WebhookActions.FETCH_WEBHOOKS_SUCCESS:
      return Object.assign({}, state, {
        isFetching: false,
        error: null,
        receivedAt: Date.now(),
        data: action.data
      });
    case WebhookActions.FETCH_WEBHOOKS_STARTED:
      // Request initiated
      return Object.assign({}, state, {
        isFetching: true,
        error: null
      });
    default:
      return state;
  }
}