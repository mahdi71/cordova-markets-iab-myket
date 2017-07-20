module.exports = {
    Initialize: function(PublicKey, Market, Success, Failed) {
		console.log("Initialize Plugin");
        cordova.exec(Success, Failed, 'MdMarkets', 'Initialize', [PublicKey, Market]); 
    },
    GetSkuDetails: function(ProductIds, Success, Failed) {
		console.log("Get Sku Details");
        cordova.exec(Success, Failed, 'MdMarkets', 'GetSkuDetails', [ProductIds]);
    },
    RequestPayment: function(ProductId, Subscribe, Success, Failed) {
		console.log("Request Payment: " + ProductId);
        cordova.exec(Success, Failed, 'MdMarkets', 'RequestPayment', [ProductId, Subscribe]);
    },
    GetOwnedProducts: function(Success, Failed) {
		console.log("Get Owned Products");
        cordova.exec(Success, Failed, 'MdMarkets', 'GetOwnedProducts', []);
    }
};