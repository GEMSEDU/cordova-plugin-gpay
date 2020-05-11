var GooglePay = {
  isReadyToPay: function () {
    return new Promise(function (resolve, reject) {
      cordova.exec(resolve, reject, 'GooglePay', 'is_ready_to_pay', [])
    })
  },
  requestPayment: function (identifier,totalPrice, currency) {
    return new Promise(function (resolve, reject) {
      cordova.exec(resolve, reject, 'GooglePay', 'request_payment', [identifier, totalPrice, currency ])
    })
  }
};

module.exports = GooglePay;
