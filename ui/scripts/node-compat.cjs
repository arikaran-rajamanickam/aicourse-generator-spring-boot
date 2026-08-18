const crypto = require('node:crypto');

if (typeof global.structuredClone !== 'function') {
  global.structuredClone = (value) => {
    const { deserialize, serialize } = require('node:v8');
    return deserialize(serialize(value));
  };
}

if (typeof crypto.getRandomValues !== 'function' && crypto.webcrypto?.getRandomValues) {
  crypto.getRandomValues = crypto.webcrypto.getRandomValues.bind(crypto.webcrypto);
}

if (!global.crypto && crypto.webcrypto) {
  global.crypto = crypto.webcrypto;
}

