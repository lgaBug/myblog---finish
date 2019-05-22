/**
 * @license Copyright (c) 2003-2018, CKSource - Frederico Knabben. All rights reserved.
 * For licensing, see https://ckeditor.com/legal/ckeditor-oss-license
 */

CKEDITOR.editorConfig = function( config ) {
	config.removeDialogTabs = 'image:advance;link:advanced';
	config.filebrowserUploadUrl = "/back/article/uploadfile?"; //上传文件保存的路径
	config.filebrowserImageUploadUrl = "/back/article/uploadfile?"; //上传图片的保存路径
	config.filebrowserFlashUploadUrl = '/back/article/uploadfile?';//上传flash的路径
};

/*
CKEDITOR_UPLOAD_PATH = '/back/article/uploadfile';
*/

/*
/!*将编辑器的语言设置为中文*!/
config.language = 'zh-cn';
/!*去掉图片预览框的文字*!/
config.image_previewText = ' ';
/!*开启工具栏“图像”中文件上传功能，后面的url为图片上传要指向的的action或servlet*!/
config.filebrowserImageUploadUrl= "/back/article/uploadfile";*/
