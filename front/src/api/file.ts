import request from '@/utils/request'
import type { UploadPictureResult } from '@/types/models'

export const uploadPicture = (file: File) => {
  const formData = new FormData()
  formData.append('pic', file)

  return request.post<UploadPictureResult>('/v1/picture', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}
