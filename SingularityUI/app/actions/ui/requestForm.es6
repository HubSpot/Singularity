import { ModifyField, ClearForm } from '../../actions/ui/form';
import { FetchRacks } from '../../actions/api/racks';
import { SaveRequest, FetchRequest } from '../../actions/api/requests';

export const refresh = (requestId, formId) => (dispatch) => {
	const promises = []

	promises.push(dispatch(FetchRacks.trigger()));

	if (requestId) {
		promises.push(dispatch(FetchRequest.trigger(requestId)));
	} else {
		promises.push(dispatch(FetchRequest.clearData()));
	}

	promises.push(dispatch(SaveRequest.clearData()));
	promises.push(dispatch(ClearForm(formId)));

	return Promise.all(promises);
};