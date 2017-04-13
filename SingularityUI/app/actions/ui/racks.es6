import { DecommissionRack, RemoveRack, ReactivateRack, FetchRacks } from '../../actions/api/racks';

export const refresh = () => (dispatch) =>
  dispatch(FetchRacks.trigger());

export const initialize = () => (dispatch) =>
  Promise.all([
    dispatch(DecommissionRack.clear()),
    dispatch(RemoveRack.clear()),
    dispatch(ReactivateRack.clear()),
    dispatch(refresh()),
  ]);
