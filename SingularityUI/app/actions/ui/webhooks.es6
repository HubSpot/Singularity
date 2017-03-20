import { FetchWebhooks } from '../../actions/api/webhooks';

export const refresh = () => (dispatch) =>
	dispatch(FetchWebhooks.trigger());