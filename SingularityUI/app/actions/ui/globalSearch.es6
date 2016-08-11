export const TOGGLE_VISIBILITY = 'TOGGLE_VISIBILITY';
export const SET_VISIBILITY = 'SET_VISIBILITY';

export const ToggleVisibility = () => {
  return (dispatch) => {
    dispatch({
      type: TOGGLE_VISIBILITY
    });
  };
};

export const SetVisibility = (visible) => {
  return (dispatch) => {
    dispatch({
      type: SET_VISIBILITY,
      value: visible
    });
  };
};
