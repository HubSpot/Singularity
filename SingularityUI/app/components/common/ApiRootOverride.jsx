import React from 'react';
import { Modal } from 'react-bootstrap';

const ApiRootOverride = () => (
  <Modal show={true}>
    <Modal.Body>
      <p>
        Hi there! I see you're running the Singularity UI locally.
        You must be trying to use a <strong>remote API</strong>.
      </p>
      <p>
        You need to specify an <strong>API root</strong> so SingularityUI knows where to get its data,
        e.g. <code>http://example/singularity/api</code>.
      </p>
      <p>This can be changed at any time in the JS console with:</p>
      <code>localStorage.setItem(&quot;apiRootOverride&quot;, &quot;http://example/singularity/api&quot;)</code>
      <p>
        Once you have one set, just refresh this page and you'll be on your way.
      </p>
    </Modal.Body>
  </Modal>
);

export default ApiRootOverride;
