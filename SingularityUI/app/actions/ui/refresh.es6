export const CANCEL_AUTO_REFRESH = 'CANCEL_AUTO_REFRESH';

export const CancelAutoRefresh = (key, isExpired = false) => {
  return (dispatch, getState) => {
    const intervalId = getState().ui.refresh[key].intervalId;

    // clear automatic expiration if this is the expiration notice
    if (!isExpired) {
      const timeoutId = getState().ui.refresh[key].timeoutId;
      if (timeoutId) {
        clearTimeout(timeoutId);
      }
    }

    // clear the actual refresh
    clearInterval(intervalId);

    dispatch({
      key,
      type: CANCEL_AUTO_REFRESH
    });
  };
};

export const BEGIN_AUTO_REFRESH = 'BEGIN_AUTO_REFRESH';

export const BeginAutoRefresh = (key, actions, interval = 30000, expiration = null) => {
  return (dispatch) => {
    const intervalId = setInterval(
      () => actions.forEach((a) => dispatch(a)),
      interval
    );

    let timeoutId;

    if (expiration) {
      timeoutId = setTimeout(() => dispatch(CancelAutoRefresh(key, true)), expiration);
    }

    dispatch({
      key,
      intervalId,
      timeoutId,
      actions,
      type: BEGIN_AUTO_REFRESH
    });
  };
};
