export default (...reducers) => (state, action) => {
  return reducers.reduce((currentState, nextReducer) => nextReducer(currentState, action), state);
};
