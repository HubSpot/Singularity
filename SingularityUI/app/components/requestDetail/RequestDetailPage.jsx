import React, { PropTypes } from 'react';

import RequestHeader from './RequestHeader';
import TaskStateBreakdown from './TaskStateBreakdown';

const RequestDetailPage = ({requestId}) => {
  return (
    <div>
      <RequestHeader requestId={requestId} />
      <TaskStateBreakdown requestId={requestId} />
    </div>
  );
};

RequestDetailPage.propTypes = {
  requestId: PropTypes.string.isRequired
};

export default RequestDetailPage;
