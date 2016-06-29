import React, { PropTypes } from 'react';
import { Row, Col } from 'react-bootstrap';

import RequestStar from '../requests/RequestStar';

const RequestHeader = ({requestParent}) => {
  return (
    <header className="detail-header">
      <Row>
        <Col md={7}>
          <h4>
            <RequestStar requestId={requestParent.request.id} />
          </h4>
        </Col>
      </Row>
    </header>
  );
};

RequestHeader.propTypes = {
  requestParent: PropTypes.object.isRequired
};

export default RequestHeader;
