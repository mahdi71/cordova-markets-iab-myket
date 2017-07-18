module.exports = {
    Initialize: function(PublicKey, Market) {
		console.log("Initialize: " + PublicKey + " .Market: " + Market);
        cordova.exec(
			function (result) {
				console.log(result);
				alert(JSON.stringify(result));
			},
			function (error) {
				console.log(error);
				alert(error);
			},
            'MdMarketsIAB',
            'Initialize',
            [PublicKey, Market]
        ); 
    },
    GetOwnedProducts: function() {
		console.log("GetOwnedProducts");
        cordova.exec(
			function (result) {
				console.log(result);
				alert(JSON.stringify(result));
			},
			function (error) {
				console.log(error);
				alert(error);
			},
            'MdMarketsIAB',
            'GetOwnedProducts',
            []
        ); 
    },
    RequestPayment: function(ProductId, ProductType, DeveloperPayload) {
		console.log("RequestPayment: " + ProductId + " .ProductType: " + ProductType);
        cordova.exec(
			function (result) {
				console.log(result);
				alert(JSON.stringify(result));
			},
			function (error) {
				console.log(error);
				alert(error);
			},
            'MdMarketsIAB',
            'RequestPayment',
            [ProductId, ProductType, DeveloperPayload]
        ); 
    },
    ConsumeProduct: function(ProductId) {
		console.log("ConsumeProduct: " + ProductId);
        cordova.exec(
			function (result) {
				console.log(result);
				alert(JSON.stringify(result));
			},
			function (error) {
				console.log(error);
				alert(error);
			},
            'MdMarketsIAB',
            'ConsumeProduct',
            [ProductId]
        ); 
    }
};