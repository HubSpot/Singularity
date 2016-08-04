import React from 'react';

export const getClickComponent = (component, doFirst) => (
  React.Children.map(component.props.children, child => (
    React.cloneElement(child, {
      onClick: () => {
        if (doFirst) {
          return doFirst().then(() => component.refs.modal.getWrappedInstance().show());
        }
        return component.refs.modal.getWrappedInstance().show();
      }
    })
  ))
);
