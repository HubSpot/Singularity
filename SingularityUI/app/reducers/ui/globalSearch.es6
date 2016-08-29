import * as GlobalSearchActions from '../../actions/ui/globalSearch';

const initialState = {
  visible: false
};

export default (state = initialState, action) => {
  switch (action.type) {
    case GlobalSearchActions.TOGGLE_VISIBILITY:
      return {
        visible: !state.visible
      };
    case GlobalSearchActions.SET_VISIBILITY:
      return {
        visible: action.value
      };
    default:
      return state;
  }
};
