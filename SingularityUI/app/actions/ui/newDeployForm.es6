import { FetchRequest } from '../../actions/api/requests';
import { ClearForm } from '../../actions/ui/form';

export const refresh = (requestId, formId) => (dispatch, getState) => {
  const promises = [];
  promises.push(dispatch(FetchRequest.trigger(requestId, true)));

  const form = getState().ui.form[formId];

  if (!form) {
    promises.push(dispatch(ClearForm(formId)));
  }

  return Promise.all(promises);
};
