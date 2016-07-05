import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { getBouncesForRequest } from '../../selectors/tasks';

import RequestHeader from './RequestHeader';

const RequestDetailPage = ({requestParent, bounces}) => {
  // if deleted handle differently here
  return (
    <div>
      <RequestHeader requestParent={requestParent} bounces={bounces} />
    </div>
  );
};

RequestDetailPage.propTypes = {
  requestParent: PropTypes.object.isRequired
};

const mapStateToProps = (state, ownProps) => {
  return {
    requestParent: state.api.request[ownProps.requestId].data,
    bounces: getBouncesForRequest(ownProps.requestId)(state)
  };
};

export default connect(
  mapStateToProps
)(RequestDetailPage);
