import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import * as StarredActions from '../../actions/ui/starred';
import { getStarred } from '../../selectors/requests';


const RequestStar = ({requestId, changeStar, starred}) => (
  <a className="star" data-starred={starred} onClick={() => changeStar(requestId)}>
    <span className="glyphicon glyphicon-star"></span>
  </a>
);

RequestStar.propTypes = {
  requestId: PropTypes.string.isRequired,
  changeStar: PropTypes.func.isRequired,
  starred: PropTypes.bool.isRequired
};

const mapStateToProps = (state, ownProps) => {
  return {
    starred: getStarred(state).has(ownProps.requestId)
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    changeStar: (requestId) => {
      dispatch(StarredActions.changeRequestStar(requestId));
    }
  };
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(RequestStar);
