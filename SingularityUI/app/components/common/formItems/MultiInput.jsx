import React, { PropTypes } from 'react';
import { FormControl } from 'react-bootstrap';
import classNames from 'classnames';

const MultiInput = (props) => {
  const change = (key, value) => {
    const valueClone = props.value.slice();
    if (key === -1) {
      valueClone.push(value);
    } else {
      valueClone[key] = value;
    }
    props.onChange(_.without(valueClone, ''));
  };

  const feedbackType = (key, value) => {
    if ((!props.feedback && _.isEmpty(props.errorIndices) && !props.doFeedback) || (key && !value)) {
      return null;
    }
    if (props.feedback) {
      return props.feedback;
    }
    if (_.isEmpty(props.errorIndices) || props.errorIndices.indexOf(key) === -1) {
      return 'SUCCESS';
    }
    return 'ERROR';
  };

  const formGroupClassNames = (key, value) => {
    const feedback = feedbackType(key, value);
    return classNames({
      'has-success': feedback === 'SUCCESS',
      'has-error': feedback === 'ERROR',
      'has-warning': feedback === 'WARN',
      'has-feedback': feedback
    });
  };

  const iconClassNames = (key, value) => {
    const feedback = feedbackType(key, value);
    return classNames(
      'glyphicon',
      'form-control-feedback',
      {
        'glyphicon-ok': feedback === 'SUCCESS',
        'glyphicon-warning-sign': feedback === 'WARN',
        'glyphicon-remove': feedback === 'ERROR'
      }
    );
  };

  const valueClone = props.value.slice();
  if (!valueClone.length || _.last(valueClone)) {
    valueClone.push('');
  }
  return (
    <div id={props.id} className={props.className}>
      {
        valueClone.map((value, key) => {
          return (
            <div className={formGroupClassNames(key, value)} key={key} >
              <FormControl
                value={value}
                onChange={(event) => change(key, event.target.value)}
                type="text"
                placeholder={props.placeholder}
              />
              {feedbackType(key, value) && <span className={iconClassNames(key, value)} />}
            </div>
          );
        })
      }
    </div>
  );
};

MultiInput.propTypes = {
  className: PropTypes.string,
  value: PropTypes.arrayOf(React.PropTypes.string).isRequired,
  onChange: PropTypes.func.isRequired, // Function of signature (newValue) => ()
  placeholder: PropTypes.string,
  errorIndices: PropTypes.arrayOf(PropTypes.number),
  id: PropTypes.string.isRequired,
  doFeedback: PropTypes.bool,
  feedback: PropTypes.oneOf(['ERROR', 'WARN', 'SUCCESS'])
};

export default MultiInput;
