import React from 'react';
import { connect } from 'react-redux';

import connectToTailer from './connectToTailer';

const Log = ({id, data}) => {
  let maybeLog;
  if (data) {
    maybeLog = <div>LOG LOADED</div>;
  } else {
    maybeLog = <div>Log not loaded</div>;
  }
  return (
    <div className="log-view">
      {id}
      {maybeLog}
    </div>
  );
};

const mapStateToProps = (state, ownProps) => ({
  data: ownProps.getTailerState(state).files[ownProps.id]
});

const mapDispatchToProps = (dispatch, ownProps) => ({
});

export default connectToTailer(connect(
  mapStateToProps,
  mapDispatchToProps
)(Log));
