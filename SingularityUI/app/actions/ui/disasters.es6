import { FetchDisabledActions, FetchDisastersData, FetchPriorityFreeze } from '../../actions/api/disasters';

export const refresh = () => (dispatch) => 
  Promise.all([
    dispatch(FetchDisabledActions.trigger()),
    dispatch(FetchDisastersData.trigger()),
    dispatch(FetchPriorityFreeze.trigger()),
  ]);